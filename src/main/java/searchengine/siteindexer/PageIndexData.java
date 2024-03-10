package searchengine.siteindexer;

import lombok.Getter;
import searchengine.services.IndexServiceImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class PageIndexData {
    private HashSet<String> visitedUrls;
    private String siteUrl;
    @Getter
    private int siteId;
    @Getter
    private String path;
    @Getter
    private IndexServiceImpl indexService;

    private PageIndexData() {
    }

    public static PageIndexData createRoot(String siteUrl, int siteId, IndexServiceImpl indexService) {
        PageIndexData root = new PageIndexData();

        root.visitedUrls = new HashSet<>();
        root.siteUrl = siteUrl;
        root.siteId = siteId;
        root.path = "/";
        root.visitedUrls.add(root.path);
        root.indexService = indexService;

        return root;
    }

    public PageIndexData createChild(String path) {
        PageIndexData child = new PageIndexData();

        child.visitedUrls = this.visitedUrls;
        child.siteUrl = this.siteUrl;
        child.indexService= this.indexService;
        child.siteId = this.siteId;
        child.path = path;

        return child;
    }

    public List<PageIndexData> processNewLinksToIndex(Collection<String> links) {
        ArrayList<String> newPatches = new ArrayList<>();

        synchronized (visitedUrls) {
            for (String link : links) {
                String path = getPathFromUrl(link);
                if (path == null) {
                    continue;
                }
                if (!visitedUrls.contains(path)) {
                    visitedUrls.add(path);
                    newPatches.add(path);
                }
            }
        }

        return newPatches.stream().map(this::createChild).toList();
    }

    public String getPathFromUrl(String url) {
        String path = url.toLowerCase();
        path = path.split("[#?]", 2)[0];

        if (path.isEmpty()) {
            return null;
        }

        if (path.equals("/")) {
            return "/";
        }

        if (path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }

        if (path.charAt(0) == '/') {
            return path;
        }

        if (path.startsWith(siteUrl + '/')) {
            return path.substring(siteUrl.length());
        }

        return null;
    }

    public String getUrl() {
        return siteUrl + path;
    }


}
