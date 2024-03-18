package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.SearchEngineException;
import searchengine.utils.LemmaFinder;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private static final String EMPTY_QUERY_MESSAGE =
            "Задан пустой поисковый запрос";
    private static final String SITE_NOT_FOUND_MESSAGE =
            "Указанная страница не найдена или не проиндексирована";
    private static final String NO_INDEXED_SITES_MESSAGE =
            "Проиндексированные страницы отсутствуют";

    private final NamedParameterJdbcTemplate template;
    private final SiteRepository siteRepository;
    private final LemmaFinder lemmaFinder;

    private static final int LIMIT_DEFAULT = 20;
    private static final int OFFSET_DEFAULT = 0;

    private static final String SEARCH_QUERY_BASE = """
            FROM page AS p
            JOIN
            	(SELECT i.page_id, COUNT(i.page_id) AS lemmas_count, SUM(i.rank) AS sum_rank
                FROM `index` AS i
            		LEFT JOIN lemma AS l ON i.lemma_id = l.id
                WHERE l.lemma IN (:LEMMAS) AND l.site_id IN (:SITES)
                GROUP BY i.page_id
                HAVING lemmas_count = :LEM_COUNT) AS idx ON idx.page_id = p.id
            LEFT JOIN site AS s ON p.site_id = s.id 
            """;

    private static final String MAIN_SEARCH_QUERY = """
            SELECT s.name as site_name, s.url as site_url, p.path as page_path,
            p.content as page_content, idx.sum_rank as page_sum_rank
            """ +
            SEARCH_QUERY_BASE + """
            ORDER BY idx.sum_rank DESC
            LIMIT :LIMIT OFFSET :OFFSET
            """;

    private static final String COUNT_SEARCH_QUERY = """
            SELECT COUNT(*) as pages_count, MAX(idx.sum_rank) as max_rank
            """ + SEARCH_QUERY_BASE;

    private static final RowMapper<SearchData> searchDataRowMapper = (resultSet, rowNum) -> {
        SearchData searchData = new SearchData();
        searchData.setSite(resultSet.getString("site_url"));
        searchData.setSiteName(resultSet.getString("site_name"));
        searchData.setUri(resultSet.getString("page_path"));
        searchData.setTitle("title");
        searchData.setSnippet(resultSet.getString("page_content"));
        searchData.setRelevance(resultSet.getDouble("page_sum_rank"));

        return searchData;
    };

    private static final RowMapper<CountAndMax> countMapper = (resultSet, rowNum) -> {
        CountAndMax countAndMax = new CountAndMax();
        countAndMax.count = resultSet.getInt("pages_count");
        countAndMax.max = resultSet.getInt("max_rank");
        return countAndMax;
    };

    @Override
    public SearchResponse getSearchResult(SearchRequest request) {
        if (Strings.isEmpty(request.getQuery())) {
            throw new SearchEngineException(EMPTY_QUERY_MESSAGE,
                    HttpStatus.BAD_REQUEST);
        }

        List<SiteEntity> indexedSites = siteRepository.findAllByStatus(
                IndexingStatus.INDEXED);

        if (indexedSites.isEmpty()) {
            throw new SearchEngineException(NO_INDEXED_SITES_MESSAGE,
                    HttpStatus.NOT_FOUND);
        }

        List<Integer> sitesToSearchIds;

        if (!Strings.isEmpty(request.getSite())) {
            Optional<SiteEntity> site = indexedSites.stream()
                    .filter(s -> s.getUrl().equals(request.getSite()))
                    .findFirst();

            if (site.isEmpty()) {
                throw new SearchEngineException(SITE_NOT_FOUND_MESSAGE,
                        HttpStatus.NOT_FOUND);
            }
            sitesToSearchIds = new ArrayList<>();
            sitesToSearchIds.add(site.get().getId());
        } else {
            sitesToSearchIds = indexedSites.stream().map(SiteEntity::getId).toList();
        }

        return searchInSiteListIds(sitesToSearchIds, request);

    }

    private static class CountAndMax {
        public int count;
        public int max;
    }


    private SearchResponse searchInSiteListIds(List<Integer> sitesIds, SearchRequest request) {
        SearchResponse result = new SearchResponse();
        result.setResult(true);

        Set<String> stringLemmas = lemmaFinder.getLemmaSet(request.getQuery());
        if (stringLemmas.isEmpty()) {
            result.setCount(0);
            return result;
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("LEMMAS", stringLemmas);
        parameters.addValue("SITES", sitesIds);
        parameters.addValue("LEM_COUNT", stringLemmas.size());

        CountAndMax countAndMax = template.query(COUNT_SEARCH_QUERY, parameters, countMapper).get(0);

        int limit = request.getLimit() == null ? LIMIT_DEFAULT : request.getLimit();
        int offset = request.getOffset() == null ? OFFSET_DEFAULT : request.getOffset();
        parameters.addValue("LIMIT", limit);
        parameters.addValue("OFFSET", offset);
        List<SearchData> pages = template.query(MAIN_SEARCH_QUERY, parameters, searchDataRowMapper);

        for (SearchData page : pages) {
            page.setRelevance(page.getRelevance()/countAndMax.max);
            setDataTitleAndSnippet(page, stringLemmas);
        }

        result.setData(pages);
        result.setCount(countAndMax.count);
        return result;
    }

    private void setDataTitleAndSnippet(SearchData data, Set<String> lemmas) {
        try {
            Document document = Jsoup.parse(data.getSnippet());
            Elements titles = document.select("title");
            if (titles != null) {
                data.setTitle(titles.get(0).text());
            }

            String[] words = lemmaFinder.arrayContainsRussianWordsOriginal(document.text());
            List<String> originalWords = new ArrayList<>();
            for (String word : words) {
                String normalWordForm = lemmaFinder.getNormalForm(word.toLowerCase());
                if (lemmas.contains(normalWordForm)) {
                    originalWords.add(word);
                }
            }

            ArrayList<String> snippets = new ArrayList<>();

            Elements allElements = document.select("*");
            boolean found = false;
            for (Element element : allElements) {
                if (Strings.isEmpty(element.ownText())) {
                    continue;
                }
                for (String word : originalWords) {
                    if (element.ownText().contains(word)) {
                        snippets.add(element.ownText());
                        break;
                    }
                }
            }

            if (snippets.isEmpty()) {
                data.setSnippet("not found");
                return;
            }

            snippets.sort(Comparator.comparing(String::length).reversed());
            String snippet = snippets.get(0);

            for (String word : originalWords) {
                snippet = snippet.replaceAll(word, "<b>" + word + "</b>");
            }
            data.setSnippet(snippet);

        } catch (Exception e) {
            data.setTitle("");
            data.setSnippet("");
        }
    }


}
