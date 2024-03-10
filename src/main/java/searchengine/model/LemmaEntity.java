package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import searchengine.config.Site;

@Entity
@Table( name = "lemma",
        uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "lemma"}))
@Getter
@Setter
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", updatable = false, insertable = false,
            foreignKey = @ForeignKey(name = "fk_lemma_site_id",
                    foreignKeyDefinition = "FOREIGN KEY (SITE_ID) REFERENCES SITE(ID) " +
                            "ON UPDATE CASCADE ON DELETE CASCADE"))
    private SiteEntity site;

    @Column(name = "site_id", nullable = false)
    private int siteId;

    @Column(name = "lemma", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

}
