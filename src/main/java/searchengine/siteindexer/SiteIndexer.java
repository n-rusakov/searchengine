package searchengine.siteindexer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import searchengine.config.Site;
import searchengine.model.IndexingStatus;
import searchengine.services.IndexServiceImpl;

import java.time.LocalDateTime;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SiteIndexer implements Runnable {
    public enum IndexStatus {NOT_STARTED, INDEXING, INTERRUPTED, DONE}

    private static final String INTERRUPT_MESSAGE = "Индексация остановлена пользователем";
    private static final String INDEXING_ERROR_MESSAGE = "Ошибка при выполнении индексирования";
    private static final String SITE_ADD_ERROR_MESSAGE = "Ошибка при вставке в таблицу Site";

    private ForkJoinPool fjp;
    @Getter
    volatile private IndexStatus status;

    private final IndexServiceImpl indexService;
    Site site;

    public SiteIndexer(Site site, IndexServiceImpl indexService) {
        this.site = site;
        this.indexService = indexService;
        this.status = IndexStatus.NOT_STARTED;
    }

    @Override
    public void run() {
        status = IndexStatus.INDEXING;
        int siteId;

        try {
            siteId = indexService.deleteOldAndCreateNewSite(site);
        } catch (Exception e) {
            log.error(SITE_ADD_ERROR_MESSAGE);
            indexService.indexingDone();
            return;
        }

        try {
            PageIndexData pageIndexData = PageIndexData.createRoot(site.getUrl(), siteId, indexService);
            PageIndexAction action = new PageIndexAction(pageIndexData);

            log.info("Indexing start: " + site.getUrl());
            fjp = new ForkJoinPool();
            fjp.invoke(action);
            log.info("Indexing ended: " + site.getUrl() + " Status: " + status);

            if (status == IndexStatus.INTERRUPTED) {
                indexService.setSiteErrorById(siteId, INTERRUPT_MESSAGE, LocalDateTime.now());
            } else {
                status = IndexStatus.DONE;
                indexService.updateSiteStatusById(siteId, IndexingStatus.INDEXED, LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error(INDEXING_ERROR_MESSAGE + " : " + e.getMessage());
            indexService.setSiteErrorById(siteId, INDEXING_ERROR_MESSAGE + " : " +
                    e.getMessage(), LocalDateTime.now());
        } finally {
            indexService.indexingDone();
        }


    }

    public void stop() {
        status = IndexStatus.INTERRUPTED;
        if (fjp != null) {
            fjp.shutdownNow();
        }
    }


}
