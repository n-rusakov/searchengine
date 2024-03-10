package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    Optional<LemmaEntity> findBySiteIdAndLemma(int siteId, String lemma);

    @Query(value = "SELECT * FROM lemma WHERE site_id=:site_id AND lemma IN (:IN)" +
            " ORDER BY frequency ASC", nativeQuery = true)
    List<LemmaEntity> getLemmasSortedByFrequency(@Param("site_id") int siteId,
                                                 @Param("IN") Set<String> LemmasINClause);


    @Modifying
    @Transactional
    @Query(value = "INSERT INTO lemma (site_id, lemma, frequency) " +
            "VALUES (:site_id, :lemma, 1) " +
            "ON DUPLICATE KEY UPDATE frequency = frequency + 1",
            nativeQuery = true)
    void insertOrUpdate(@Param("site_id") int site_id,
                          @Param("lemma") String lemma);


}
