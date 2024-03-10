package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.exceptions.SearchEngineException;
import searchengine.lemmafinder.LemmaFinder;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.siteindexer.PageIndexAction;
import searchengine.siteindexer.SiteIndexer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexServiceImpl implements IndexService {
    private final SitesList sites;

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private final LemmaFinder lemmaFinder;

    private boolean indexing = false;
    private volatile AtomicInteger indexersCount;
    ArrayList<SiteIndexer> siteIndexers;

    private static final String ALREADY_INDEXING_MESSAGE = "Индексация уже запущена";
    private static final String NO_INDEXING_MESSAGE = "Индексация не запущена";
    private static final String NO_PAGE_IN_CONFIG_MESSAGE = "Данная страница находится " +
            "за пределами сайтов, указанных в конфигурационном файле";

    @Override
    public void startIndexing() {
        if (isIndexing()) {
            throw new SearchEngineException(ALREADY_INDEXING_MESSAGE, HttpStatus.FORBIDDEN);
        }
        siteIndexers = new ArrayList<>();

        for (Site site : sites.getSites()) {
            Site normalizedSite = new Site();
            normalizedSite.setUrl(normalizeRoot(site.getUrl()));
            normalizedSite.setName(site.getName());

            SiteIndexer siteIndexer = new SiteIndexer(normalizedSite, this);
            siteIndexers.add(siteIndexer);
        }

        indexing = true;
        for (SiteIndexer indexer : siteIndexers) {
            new Thread(indexer).start();
        }
    }

    @Override
    public void stopIndexing() {
        if (!isIndexing()) {
            throw new SearchEngineException(NO_INDEXING_MESSAGE, HttpStatus.FORBIDDEN);
        }

        for (SiteIndexer indexer : siteIndexers) {
            indexer.stop();
        }
        indexing = false;
    }

    @Override
    public boolean isIndexing() {
        return indexing;
        /*if (!indexing) {
            return false;
        }

        for (SiteIndexer indexer : siteIndexers) {
            if (indexer.getStatus() == SiteIndexer.IndexStatus.INDEXING) {
                return true;
            }
        }

        indexing = false;
        return false; */
    }

    public void indexingDone() {
        if (indexersCount.decrementAndGet() == 0) {
            indexing = false;
        }
    }

    @Override
    public void startPageIndexing(String url) {
        if (isIndexing()) {
            throw new SearchEngineException(ALREADY_INDEXING_MESSAGE, HttpStatus.FORBIDDEN);
        }

        Site rootSite = null;
        url = url.toLowerCase();
        for (Site site : sites.getSites()) {
            site.setUrl(normalizeRoot(site.getUrl()));
            if (url.startsWith(site.getUrl())) {
                rootSite = site;
                break;
            }
        }

        if (rootSite == null) {
            throw new SearchEngineException(NO_PAGE_IN_CONFIG_MESSAGE, HttpStatus.NOT_FOUND);
        }

        List<SiteEntity> siteEntities = siteRepository.findAllByUrl(rootSite.getUrl());
        int siteId;
        if (siteEntities.isEmpty()) {
            SiteEntity newSite = new SiteEntity();
            newSite.setUrl(rootSite.getUrl());
            newSite.setName(rootSite.getName());
            newSite.setStatus(IndexingStatus.INDEXING);
            newSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(newSite);
            siteId = newSite.getId();
        } else {
            siteId = siteEntities.get(0).getId();
        }

        String path = url.substring(rootSite.getUrl().length() - 1);
        indexSinglePage(siteId, url, path);
    }


    public void indexSinglePage(int siteId, String url, String path) {
        try {
            Connection.Response response = PageIndexAction.getResponse(url);
            if (response == null || response.contentType() == null ||
                    !response.contentType().contains("html") || response.statusCode()>=400) {
                return;
            }
            String pageText = response.parse().text();

            Optional<PageEntity> oldPageEntity = pageRepository.findBySiteIdAndPath(siteId, path);
            oldPageEntity.ifPresent(pageRepository::delete);

            PageEntity pageEntity = new PageEntity();
            pageEntity.setCode(response.statusCode());
            pageEntity.setContent(response.body());
            pageEntity.setPath(path);
            pageEntity.setSiteId(siteId);
            pageRepository.save(pageEntity);
            int pageId = pageEntity.getId();

            saveLemmas(siteId, pageId, pageText);

        } catch (Exception e) {
            //
        }

    }

    public int deleteOldAndCreateNewSite(Site site) {
        List<SiteEntity> oldSites = siteRepository.findAllByUrl(site.getUrl());

        for (SiteEntity oldSite : oldSites) {
            siteRepository.delete(oldSite);
        }

        SiteEntity newSite = new SiteEntity();
        newSite.setUrl(site.getUrl());
        newSite.setName(site.getName());
        newSite.setStatus(IndexingStatus.INDEXING);
        newSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(newSite);

        return  newSite.getId();
    }

    public void updateSiteStatusTimeById(int id, LocalDateTime time){
        siteRepository.updateStatusTimeById(id, time);
    }

    public void updateSiteStatusById(int id, IndexingStatus status, LocalDateTime time) {
        siteRepository.updateStatusById(id, status.toString(), time);
    }

    public void setSiteErrorById(int id, String error, LocalDateTime time) {
        siteRepository.updateStatusAndErrorById(id, IndexingStatus.FAILED.toString(),
                time, error);
    }

    public int addPageBySiteId(Integer siteId, String path, int responseCode, String content){
        PageEntity pageEntity = new PageEntity();
        pageEntity.setCode(responseCode);
        pageEntity.setContent(content);
        pageEntity.setPath(path);
        pageEntity.setSiteId(siteId);
        pageRepository.save(pageEntity);

        return pageEntity.getId();
    }

    private void saveIndex(int pageId, int lemmaId, float rank) {
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setPageId(pageId);
        indexEntity.setLemmaId(lemmaId);
        indexEntity.setRank(rank);
        indexRepository.save(indexEntity);
    }

    @Transactional()
    private Integer saveLemma(int siteId, String lemma) {

        lemmaRepository.insertOrUpdate(siteId, lemma);
        LemmaEntity lemmaEntity =
                lemmaRepository.findBySiteIdAndLemma(siteId, lemma).orElse(null);
        if (lemmaEntity != null) {
            return lemmaEntity.getId();
        } else {
            log.info("Lemma not saved: " + lemma + ". Site id: " + siteId);
            return null;
        }
    }

    public void saveLemmas(int siteId, int pageId, String content) {
        Map<String, Integer> lemmas =
                lemmaFinder.collectLemmas(Jsoup.parse(content).text());

        if (lemmas.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Integer> lemma: lemmas.entrySet()) {
            Integer lemmaId = saveLemma(siteId, lemma.getKey());
            if (lemmaId != null) {
                saveIndex(pageId, lemmaId, lemma.getValue());
            }

        }

    }


    private String normalizeRoot(String url) {
        url = url.toLowerCase();
        if (url.length() < 2) {
            return url;
        }
        return url.charAt(url.length() - 1) != '/' ? url :
                url.substring(0, url.length() - 1);
    }

}
