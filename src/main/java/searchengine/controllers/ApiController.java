package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexPageRequestDto;
import searchengine.dto.indexing.IndexingResultResponse;
import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexService indexService;
    private final SearchService searchService;


    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    public IndexingResultResponse startIndexing() {
        indexService.startIndexing();
        return new IndexingResultResponse();
    }

    @GetMapping("/stopIndexing")
    public void stopIndexing() {
        indexService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public IndexingResultResponse startPageIndex(@RequestParam String url) {
        indexService.startPageIndexing(url);
        return new IndexingResultResponse();
    }

    @GetMapping("/search")
    public SearchResponse search(SearchRequest request) {
        return searchService.getSearchResult(request);
    }
}
