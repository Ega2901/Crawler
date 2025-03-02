package com.example.crawler;

import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class NYTimesCrawler extends WebCrawler {
    private static final String NEWS_SITE_DOMAIN = "nytimes.com";
    private static final String NEWS_SITE_NAME = "NYTimes";
    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g|png|tiff?|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf|rm|smil|wmv|swf|wma|zip|rar|gz))$");
    private CrawlData localData = new CrawlData();

    @Override
    protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
        localData.fetchDataList.add(new FetchData(webUrl.getURL(), statusCode));
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        String indicator = href.contains(NEWS_SITE_DOMAIN) ? "OK" : "N_OK";
        localData.urlDataList.add(new UrlData(url.getURL(), indicator));
        return !FILTERS.matcher(href).matches() && href.contains(NEWS_SITE_DOMAIN);
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        int fileSize = (page.getContentData() != null) ? page.getContentData().length : 0;
        String contentType = page.getContentType();
        // Убираем charset, если он присутствует (например, "text/html; charset=UTF-8")
        if (contentType != null && contentType.contains(";")) {
            contentType = contentType.split(";")[0].trim();
        }
        int numOutlinks = 0;
        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            List<WebURL> outlinks = new ArrayList<>(htmlParseData.getOutgoingUrls());
            numOutlinks = outlinks.size();
        }
        localData.visitDataList.add(new VisitData(url, fileSize, numOutlinks, contentType));
    }

    @Override
    public Object getMyLocalData() {
        return localData;
    }

    public static class CrawlData {
        public List<FetchData> fetchDataList = new ArrayList<>();
        public List<VisitData> visitDataList = new ArrayList<>();
        public List<UrlData> urlDataList = new ArrayList<>();
    }

    public static class FetchData {
        public String url;
        public int statusCode;
        public FetchData(String url, int statusCode) {
            this.url = url;
            this.statusCode = statusCode;
        }
    }

    public static class VisitData {
        public String url;
        public int fileSize;
        public int outLinks;
        public String contentType;
        public VisitData(String url, int fileSize, int outLinks, String contentType) {
            this.url = url;
            this.fileSize = fileSize;
            this.outLinks = outLinks;
            this.contentType = contentType;
        }
    }

    public static class UrlData {
        public String url;
        public String indicator;
        public UrlData(String url, String indicator) {
            this.url = url;
            this.indicator = indicator;
        }
    }
}