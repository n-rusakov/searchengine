package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "site")
@Getter
@Setter
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IndexingStatus status;

    @Column(name = "status_time")
    private LocalDateTime statusTime;

    /*@OneToMany(mappedBy = "siteEntity", fetch = FetchType.EAGER)
    private List<PageEntity> pages;*/

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "name", nullable = false)
    private String name;
}
