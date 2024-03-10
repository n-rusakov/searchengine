package searchengine.services;

import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;

public interface SearchService {
    SearchResponse getSearchResult(SearchRequest request);
}
