package afg.process.afgprocess.model.procedure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Question de la banque de questions d'une procédure (QCM, une seule bonne réponse).
 * Chaque quiz tire au hasard des questions actives de la procédure.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_question")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_procedure", nullable = false)
    private Procedure procedure;

    @Column(nullable = false, length = 1000)
    private String enonce;

    /** Une question désactivée n'est plus tirée dans les quiz (l'historique est conservé). */
    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private boolean actif = true;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Choix> choix = new ArrayList<>();

    public void ajouterChoix(Choix c) {
        c.setQuestion(this);
        choix.add(c);
    }
}
