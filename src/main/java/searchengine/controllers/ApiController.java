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
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResultResponse> startIndexing() {
        indexService.startIndexing();
        return ResponseEntity.ok(new IndexingResultResponse());
    }

    @GetMapping("/stopIndexing")
    public void stopIndexing() {
        indexService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResultResponse> startPageIndex(
            @RequestParam String url) {
        indexService.startPageIndexing(url);
        return ResponseEntity.ok(new IndexingResultResponse());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(SearchRequest request) {
        return ResponseEntity.ok(searchService.getSearchResult(request));
    }
}
