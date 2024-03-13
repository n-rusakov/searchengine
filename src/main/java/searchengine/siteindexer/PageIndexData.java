package searchengine.siteindexer;

import lombok.Getter;
import searchengine.services.IndexServiceImpl;

import java.util.*;
import java.util.stream.Collectors;

public class PageIndexData {
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

        root.siteUrl = siteUrl;
        root.siteId = siteId;
        root.path = "/";
        root.indexService = indexService;

        return root;
    }

    public PageIndexData createChild(String path) {
        PageIndexData child = new PageIndexData();

        child.siteUrl = this.siteUrl;
        child.indexService = this.indexService;
        child.siteId = this.siteId;
        child.path = path;

        return child;
    }

    public List<PageIndexData> processNewLinksToIndex(Collection<String> links) {
        ArrayList<String> newPatches = new ArrayList<>();
        Set<String> patches = links.stream()
                .map(this::getPathFromUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String path : patches) {
            boolean pageExist = indexService.isPageExist(this.siteId, path);
            if (!pageExist) {
                newPatches.add(path);
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
