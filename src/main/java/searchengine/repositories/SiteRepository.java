package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;

import java.time.LocalDateTime;
import java.util.List;



@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    List<SiteEntity> findAllByUrl(String url);
    List<SiteEntity> findAllByStatus(IndexingStatus status);

    @Modifying
    @Transactional
    @Query(value = "UPDATE site SET status_time = :new_time WHERE id = :id",
            nativeQuery = true)
    void updateStatusTimeById(@Param("id") int id,
                              @Param("new_time") LocalDateTime newTime);

    @Modifying
    @Transactional
    @Query(value = "UPDATE site SET status = :status, status_time = :time WHERE id = :id",
            nativeQuery = true)
    void updateStatusById(@Param("id") int id,
                          @Param("status") String status,
                          @Param("time") LocalDateTime time);

    @Modifying
    @Transactional
    @Query(value = "UPDATE site SET status = :status, status_time = :time, " +
            "last_error = :error WHERE id = :id",
            nativeQuery = true)
    void updateStatusAndErrorById(@Param("id") int id,
                                  @Param("status") String status,
                                  @Param("time") LocalDateTime time,
                                  @Param("error") String error);

}
