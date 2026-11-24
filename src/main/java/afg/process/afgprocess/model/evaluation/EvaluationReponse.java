package afg.process.afgprocess.model.evaluation;

import afg.process.afgprocess.model.procedure.Choix;
import afg.process.afgprocess.model.procedure.Question;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Question tirée pour une évaluation, et la réponse donnée par le collaborateur. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "evaluation_reponse")
public class EvaluationReponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluation_reponse")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evaluation", nullable = false)
    private Evaluation evaluation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_question", nullable = false)
    private Question question;

    /** Choix sélectionné ; null tant que le quiz n'est pas soumis, ou si la question est restée sans réponse. */
    @ManyToOne
    @JoinColumn(name = "id_choix")
    private Choix choix;

    private Boolean correct;

    /** Position de la question dans le quiz (1 à n). */
    @Column(nullable = false)
    private int ordre;
}
