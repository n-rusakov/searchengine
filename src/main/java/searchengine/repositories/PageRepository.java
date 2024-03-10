package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    @Modifying
    @Query(value = "delete from page where site_id = :site_id", nativeQuery = true)
    void deleteBySiteId(@Param("site_id") int siteId);

    Optional<PageEntity> findBySiteIdAndPath(int siteId, String path);

}
