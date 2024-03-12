package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final IndexService indexService;

    private final JdbcTemplate jdbcTemplate;

    private static final String STATISTICS_QUERY = """
            SELECT  s.url as url,
                    s.name as name,
                    s.status as status,
            	    s.status_time as status_time,
            	    s.last_error as error,
            	        (SELECT count(*) FROM page as p
            	        WHERE p.site_id = s.id) as pages,
            	        (SELECT count(*) FROM lemma as l
            	        WHERE l.site_id = s.id) as lemmas
            FROM site as s
            """;

    private static final RowMapper<DetailedStatisticsItem>
            rowMapper = (resultSet, rowNum) -> {
        DetailedStatisticsItem item = new DetailedStatisticsItem();

        item.setUrl(resultSet.getString("url"));
        item.setName(resultSet.getString("name"));
        item.setStatus(resultSet.getString("status"));

        LocalDateTime time = resultSet.getObject("status_time",
                LocalDateTime.class);
        ZonedDateTime zdt = ZonedDateTime.of(time, ZoneId.systemDefault());
        item.setStatusTime(zdt.toInstant().toEpochMilli());

        String error = resultSet.getString("error");
        item.setError(error != null ? error : "");
        item.setPages(resultSet.getInt("pages"));
        item.setLemmas(resultSet.getInt("lemmas"));

        return item;
    } ;


    @Override
    public StatisticsResponse getStatistics() {
        List<DetailedStatisticsItem> queriedDetailed =
                jdbcTemplate.query(STATISTICS_QUERY, rowMapper);

        int totalPages = 0;
        int totalLemmas = 0;
        for (DetailedStatisticsItem item : queriedDetailed) {
            totalPages += item.getPages();
            totalLemmas += item.getLemmas();
        }

        TotalStatistics total = new TotalStatistics();
        total.setIndexing(indexService.isIndexing());
        total.setSites(queriedDetailed.size());
        total.setPages(totalPages);
        total.setLemmas(totalLemmas);

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(queriedDetailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
