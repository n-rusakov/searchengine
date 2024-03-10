package searchengine.services;

public interface IndexService {
    void startIndexing();
    void stopIndexing();

    void startPageIndexing(String url);
    boolean isIndexing();
}
