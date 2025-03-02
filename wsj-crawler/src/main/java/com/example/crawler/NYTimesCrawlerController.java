package com.example.crawler;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

public class NYTimesCrawlerController {
    private static final String NEWS_SITE_NAME = "NYTimes";
    private static final String SEED_URL = "https://www.nytimes.com/";
    private static final int NUM_CRAWLERS = 7;

    public static void main(String[] args) throws Exception {
        String crawlStorageFolder = "data/crawl/root";
        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);
        config.setPolitenessDelay(200);
        config.setMaxDepthOfCrawling(16);
        config.setMaxPagesToFetch(10000);
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
        controller.addSeed(SEED_URL);
        controller.start(NYTimesCrawler.class, NUM_CRAWLERS);

        List<Object> crawlersLocalData = controller.getCrawlersLocalData();
        List<NYTimesCrawler.FetchData> allFetchData = new ArrayList<>();
        List<NYTimesCrawler.VisitData> allVisitData = new ArrayList<>();
        List<NYTimesCrawler.UrlData> allUrlData = new ArrayList<>();
        for (Object localDataObj : crawlersLocalData) {
            NYTimesCrawler.CrawlData data = (NYTimesCrawler.CrawlData) localDataObj;
            allFetchData.addAll(data.fetchDataList);
            allVisitData.addAll(data.visitDataList);
            allUrlData.addAll(data.urlDataList);
        }

        writeFetchCSV(allFetchData);
        writeVisitCSV(allVisitData);
        writeUrlsCSV(allUrlData);
        generateCrawlReport(allFetchData, allVisitData, allUrlData, NUM_CRAWLERS);
    }

    private static void writeFetchCSV(List<NYTimesCrawler.FetchData> fetchData) {
        String filename = "fetch_" + NEWS_SITE_NAME + ".csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("URL,Status");
            int count = 0;
            for (NYTimesCrawler.FetchData data : fetchData) {
                if (count >= 20000) break;
                writer.println(data.url + "," + data.statusCode);
                count++;
            }
            System.out.println("Wrote " + count + " rows to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeVisitCSV(List<NYTimesCrawler.VisitData> visitData) {
        String filename = "visit_" + NEWS_SITE_NAME + ".csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("URL,Size(Bytes),Outlinks,ContentType");
            for (NYTimesCrawler.VisitData data : visitData) {
                writer.println(data.url + "," + data.fileSize + "," + data.outLinks + "," + data.contentType);
            }
            System.out.println("Wrote " + visitData.size() + " rows to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeUrlsCSV(List<NYTimesCrawler.UrlData> urlData) {
        String filename = "urls_" + NEWS_SITE_NAME + ".csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("URL,Indicator");
            for (NYTimesCrawler.UrlData data : urlData) {
                writer.println(data.url + "," + data.indicator);
            }
            System.out.println("Wrote " + urlData.size() + " rows to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateCrawlReport(List<NYTimesCrawler.FetchData> fetchData,
                                              List<NYTimesCrawler.VisitData> visitData,
                                              List<NYTimesCrawler.UrlData> urlData,
                                              int numThreads) {
        int fetchesAttempted = fetchData.size();
        int fetchesSucceeded = 0;
        int fetchesFailed = 0;
        Map<Integer, Integer> statusCodeCounts = new HashMap<>();
        for (NYTimesCrawler.FetchData fd : fetchData) {
            int code = fd.statusCode;
            statusCodeCounts.put(code, statusCodeCounts.getOrDefault(code, 0) + 1);
            if (code >= 200 && code < 300) {
                fetchesSucceeded++;
            } else {
                fetchesFailed++;
            }
        }
        int totalOutlinks = 0;
        for (NYTimesCrawler.VisitData vd : visitData) {
            totalOutlinks += vd.outLinks;
        }
        Set<String> uniqueUrls = new HashSet<>();
        Set<String> uniqueWithin = new HashSet<>();
        Set<String> uniqueOutside = new HashSet<>();
        for (NYTimesCrawler.UrlData ud : urlData) {
            uniqueUrls.add(ud.url);
            if ("OK".equals(ud.indicator))
                uniqueWithin.add(ud.url);
            else
                uniqueOutside.add(ud.url);
        }
        int filesLessThan1KB = 0, files1KBto10KB = 0, files10KBto100KB = 0, files100KBto1MB = 0, filesLargerThan1MB = 0;
        for (NYTimesCrawler.VisitData vd : visitData) {
            int size = vd.fileSize;
            if (size < 1024) filesLessThan1KB++;
            else if (size < 10 * 1024) files1KBto10KB++;
            else if (size < 100 * 1024) files10KBto100KB++;
            else if (size < 1024 * 1024) files100KBto1MB++;
            else filesLargerThan1MB++;
        }
        Map<String, Integer> contentTypeCounts = new HashMap<>();
        for (NYTimesCrawler.VisitData vd : visitData) {
            String ct = vd.contentType;
            contentTypeCounts.put(ct, contentTypeCounts.getOrDefault(ct, 0) + 1);
        }
        String reportFilename = "CrawlReport_" + NEWS_SITE_NAME + ".txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFilename))) {
            writer.println("News site crawled: " + "nytimes.com");
            writer.println("Number of threads: " + numThreads);
            writer.println("Fetch Statistics");
            writer.println("# fetches total: " + fetchesAttempted);
            writer.println("# fetches succeeded: " + fetchesSucceeded);
            writer.println("# fetches failed or aborted: " + fetchesFailed);
            writer.println("Outgoing URLs:");
            writer.println("==============");
            writer.println("Total URLs extracted: " + totalOutlinks);
            writer.println("# unique URLs extracted: " + uniqueUrls.size());
            writer.println("# unique URLs within News Site: " + uniqueWithin.size());
            writer.println("# unique URLs outside News Site: " + uniqueOutside.size());
            writer.println("Status Codes:");
            writer.println("==============");
            if (statusCodeCounts.containsKey(200))
                writer.println("200 OK: " + statusCodeCounts.get(200));
            if (statusCodeCounts.containsKey(301))
                writer.println("301 Moved Permanently: " + statusCodeCounts.get(301));
            if (statusCodeCounts.containsKey(302))
                writer.println("302 Moved Temporarily: " + statusCodeCounts.get(302));
            if (statusCodeCounts.containsKey(404))
                writer.println("404 Resource Not Found: " + statusCodeCounts.get(404));
            writer.println("File Sizes:");
            writer.println("==============");
            writer.println("< 1KB: " + filesLessThan1KB);
            writer.println("1KB ~ <10KB: " + files1KBto10KB);
            writer.println("10KB ~ <100KB: " + files10KBto100KB);
            writer.println("100KB ~ <1MB: " + files100KBto1MB);
            writer.println(">= 1MB: " + filesLargerThan1MB);
            writer.println("Content Types:");
            writer.println("==============");
            for (Map.Entry<String, Integer> entry : contentTypeCounts.entrySet()) {
                writer.println(entry.getKey() + ": " + entry.getValue());
            }
            System.out.println("Wrote crawl report to " + reportFilename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}