package org.schabi.newpipe.extractor.services.youtube;

import org.junit.BeforeClass;
import org.junit.Test;
import org.schabi.newpipe.downloader.DownloaderTestImpl;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeSubscriptionExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionItem;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.schabi.newpipe.FileUtils.resolveTestResource;
import static org.schabi.newpipe.extractor.utils.Utils.UTF_8;

/**
 * Test for {@link YoutubeSubscriptionExtractor}
 */
public class YoutubeSubscriptionExtractorTest {


    private static YoutubeSubscriptionExtractor subscriptionExtractor;
    private static LinkHandlerFactory urlHandler;

    @BeforeClass
    public static void setupClass() {
        //Doesn't make network requests
        NewPipe.init(DownloaderTestImpl.getInstance());
        subscriptionExtractor = new YoutubeSubscriptionExtractor(ServiceList.YouTube);
        urlHandler = ServiceList.YouTube.getChannelLHFactory();
    }

    @Test
    public void testFromInputStream() throws Exception {
        final List<SubscriptionItem> subscriptionItems = subscriptionExtractor.fromInputStream(
                new FileInputStream(resolveTestResource("youtube_takeout_import_test.json")));
        assertEquals(7, subscriptionItems.size());

        for (final SubscriptionItem item : subscriptionItems) {
            assertNotNull(item.getName());
            assertNotNull(item.getUrl());
            assertTrue(urlHandler.acceptUrl(item.getUrl()));
            assertEquals(ServiceList.YouTube.getServiceId(), item.getServiceId());
        }
    }

    @Test
    public void testEmptySourceException() throws Exception {
        final List<SubscriptionItem> items = subscriptionExtractor.fromInputStream(
                new ByteArrayInputStream("[]".getBytes(UTF_8)));
        assertTrue(items.isEmpty());
    }

    @Test
    public void testSubscriptionWithEmptyTitleInSource() throws Exception {
        final String source = "[{\"snippet\":{\"resourceId\":{\"channelId\":\"UCEOXxzW2vU0P-0THehuIIeg\"}}}]";
        final List<SubscriptionItem> items = subscriptionExtractor.fromInputStream(
                new ByteArrayInputStream(source.getBytes(UTF_8)));

        assertEquals(1, items.size());
        assertEquals(ServiceList.YouTube.getServiceId(), items.get(0).getServiceId());
        assertEquals("https://www.youtube.com/channel/UCEOXxzW2vU0P-0THehuIIeg", items.get(0).getUrl());
        assertEquals("", items.get(0).getName());
    }

    @Test
    public void testSubscriptionWithInvalidUrlInSource() throws Exception {
        final String source = "[{\"snippet\":{\"resourceId\":{\"channelId\":\"gibberish\"},\"title\":\"name1\"}}," +
                "{\"snippet\":{\"resourceId\":{\"channelId\":\"UCEOXxzW2vU0P-0THehuIIeg\"},\"title\":\"name2\"}}]";
        final List<SubscriptionItem> items = subscriptionExtractor.fromInputStream(
                new ByteArrayInputStream(source.getBytes(UTF_8)));

        assertEquals(1, items.size());
        assertEquals(ServiceList.YouTube.getServiceId(), items.get(0).getServiceId());
        assertEquals("https://www.youtube.com/channel/UCEOXxzW2vU0P-0THehuIIeg", items.get(0).getUrl());
        assertEquals("name2", items.get(0).getName());
    }

    @Test
    public void testInvalidSourceException() {
        List<String> invalidList = Arrays.asList(
                "<xml><notvalid></notvalid></xml>",
                "<opml><notvalid></notvalid></opml>",
                "{\"a\":\"b\"}",
                "[{}]",
                "[\"\", 5]",
                "[{\"snippet\":{\"title\":\"name\"}}]",
                "[{\"snippet\":{\"resourceId\":{\"channelId\":\"gibberish\"}}}]",
                "",
                "\uD83D\uDC28\uD83D\uDC28\uD83D\uDC28",
                "gibberish");

        for (String invalidContent : invalidList) {
            try {
                byte[] bytes = invalidContent.getBytes(UTF_8);
                subscriptionExtractor.fromInputStream(new ByteArrayInputStream(bytes));
                fail("Extracting from \"" + invalidContent + "\" didn't throw an exception");
            } catch (final Exception e) {
                boolean correctType = e instanceof SubscriptionExtractor.InvalidSourceException;
                if (!correctType) {
                    e.printStackTrace();
                }
                assertTrue(e.getClass().getSimpleName() + " is not InvalidSourceException", correctType);
            }
        }
    }
}
