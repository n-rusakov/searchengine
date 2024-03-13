package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "page")
@Getter
@Setter
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", updatable = false, insertable = false,
            foreignKey = @ForeignKey(name = "fk_page_site_id",
            foreignKeyDefinition = "FOREIGN KEY (site_id) REFERENCES site(id) " +
                    "ON UPDATE CASCADE ON DELETE CASCADE"))
    private SiteEntity siteEntity;

    @Column(name = "site_id", nullable = false)
    private int siteId;

    @Column(name = "`path`", columnDefinition = "TEXT, " +
            "CONSTRAINT page_site_path_unique UNIQUE (site_id, path(50))")
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;
}
