package searchengine.siteindexer;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.services.IndexServiceImpl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RecursiveAction;

@RequiredArgsConstructor
public class PageIndexAction extends RecursiveAction {
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
    private static final String REFERRER = "http://www.google.com";
    private static final int PARSE_DELAY_MS = 500;

    private final PageIndexData pageIndexData;

    @Override
    protected void compute() {
        try {
            Connection.Response response = getResponse(pageIndexData.getUrl());
            if (response == null || response.contentType() == null ||
                    !response.contentType().contains("html") || response.statusCode()>=400) {
                return;
            }
            Document document = response.parse();
            IndexServiceImpl service = pageIndexData.getIndexService();
            // handle page content
            int pageId = service.addPageBySiteId(pageIndexData.getSiteId(),
                    pageIndexData.getPath(), response.statusCode(), response.body());

            service.saveLemmas(pageIndexData.getSiteId(), pageId, document.text());

            // update site status_time
            service.updateSiteStatusTimeById(pageIndexData.getSiteId(),
                    LocalDateTime.now());


            List<PageIndexData> pagesToIndex = pageIndexData.
                    processNewLinksToIndex(getLinks(document));

            List<PageIndexAction> actions = pagesToIndex.stream().map(PageIndexAction::new).toList();
            invokeAll(actions);
        } catch (CancellationException | IOException e) {
            // do nothing
        }

    }

    private static Set<String> getLinks(Document document) {
        Set<String> result = new HashSet<>();

        Elements linkElements = document.select("a");
        for (Element elem : linkElements) {
            result.add(elem.attr("href"));
        }
        return result;
    }

    public static Connection.Response getResponse(String url) {
        try {
            Thread.sleep(PARSE_DELAY_MS);
        } catch (InterruptedException e) {
            return null;
        }

        Connection.Response response;
        try {
            response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .referrer(REFERRER)
                    .execute();

            return response;
        } catch (Exception e) {
            return  null;
        }
    }

}
