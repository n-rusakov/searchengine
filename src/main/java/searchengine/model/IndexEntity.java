package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "`index`")
@Getter
@Setter
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", updatable = false, insertable = false,
            foreignKey = @ForeignKey(name = "fk_index_page_id",
                    foreignKeyDefinition = "FOREIGN KEY (page_id) REFERENCES page(id) " +
                            "ON UPDATE CASCADE ON DELETE CASCADE"))
    private PageEntity page;

    @Column(name = "page_id", nullable = false)
    private int pageId;


    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", updatable = false, insertable = false,
            foreignKey = @ForeignKey(name = "fk_index_lemma_id",
                    foreignKeyDefinition = "FOREIGN KEY (lemma_id) REFERENCES lemma(id) " +
                            "ON UPDATE CASCADE ON DELETE CASCADE"))
    private LemmaEntity lemma;

    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;

    @Column(name = "`rank`", nullable = false)
    private float rank;
}
