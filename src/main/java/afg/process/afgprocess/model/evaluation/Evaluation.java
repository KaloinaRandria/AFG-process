package afg.process.afgprocess.model.evaluation;

import afg.process.afgprocess.model.procedure.Procedure;
import afg.process.afgprocess.model.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Passage d'un quiz par un collaborateur sur une procédure.
 * Les questions sont tirées au démarrage (lignes EvaluationReponse sans réponse),
 * puis la note et la décision sont calculées à la soumission.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "evaluation", indexes = {
        @Index(name = "idx_evaluation_utilisateur_procedure", columnList = "id_utilisateur, id_procedure")
})
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluation")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_procedure", nullable = false)
    private Procedure procedure;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutEvaluation statut = StatutEvaluation.EN_COURS;

    @Column(name = "nb_questions", nullable = false)
    private int nbQuestions;

    /** Seuil de réussite de la procédure au moment du quiz (il peut changer ensuite). */
    @Column(nullable = false)
    private int seuil;

    /** Nombre de bonnes réponses (renseigné à la soumission). */
    private Integer note;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ResultatEvaluation resultat;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre")
    private List<EvaluationReponse> reponses = new ArrayList<>();

    public boolean isTerminee() {
        return statut == StatutEvaluation.TERMINEE;
    }

    public boolean isFormationRequise() {
        return resultat == ResultatEvaluation.FORMATION_REQUISE;
    }

    /** Note ramenée sur 20 (affichage). */
    public Double getNoteSur20() {
        return note == null || nbQuestions == 0 ? null : Math.round(note * 200.0 / nbQuestions) / 10.0;
    }
}
