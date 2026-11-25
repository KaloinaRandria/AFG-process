package afg.process.afgprocess.repository.specification;

import afg.process.afgprocess.dto.filtre.EvaluationFiltre;
import afg.process.afgprocess.model.evaluation.Evaluation;
import afg.process.afgprocess.model.evaluation.StatutEvaluation;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/** Recherche multicritère des quiz terminés. */
public final class EvaluationSpecifications {

    private EvaluationSpecifications() {
    }

    /**
     * @param idUtilisateur     limite aux quiz de ce collaborateur (null = tous)
     * @param derniersSeulement ne garde que le dernier quiz de chaque collaborateur sur chaque procédure
     */
    public static Specification<Evaluation> filtre(EvaluationFiltre f, Integer idUtilisateur, boolean derniersSeulement) {
        return Criteres.<Evaluation>pour()
                .egal("statut", StatutEvaluation.TERMINEE)
                .egal("utilisateur.id", idUtilisateur)
                .contient(f.getQ(), "procedure.reference", "procedure.titre")
                .contient(f.getCollaborateur(), "utilisateur.nom", "utilisateur.prenom", "utilisateur.mail")
                .egal("procedure.direction.id", f.getDirection())
                .egal("utilisateur.direction.id", f.getDirectionCollaborateur())
                .egal("procedure.id", f.getProcedure())
                .egal("resultat", f.getResultat())
                .entre("dateFin", f.getDu(), f.getAu())
                .si(derniersSeulement, dernierQuiz())
                .construire();
    }

    /** Le quiz est le plus récent du collaborateur sur cette procédure. */
    private static Specification<Evaluation> dernierQuiz() {
        return (root, query, cb) -> {
            Subquery<LocalDateTime> max = query.subquery(LocalDateTime.class);
            Root<Evaluation> e2 = max.from(Evaluation.class);
            max.select(cb.greatest(e2.<LocalDateTime>get("dateFin"))).where(
                    cb.equal(e2.get("utilisateur"), root.get("utilisateur")),
                    cb.equal(e2.get("procedure"), root.get("procedure")),
                    cb.equal(e2.get("statut"), StatutEvaluation.TERMINEE));
            return cb.equal(root.get("dateFin"), max);
        };
    }
}
