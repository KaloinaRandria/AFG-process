package afg.process.afgprocess.repository.specification;

import afg.process.afgprocess.dto.filtre.ProcedureFiltre;
import afg.process.afgprocess.model.evaluation.Evaluation;
import afg.process.afgprocess.model.evaluation.ResultatEvaluation;
import afg.process.afgprocess.model.evaluation.StatutEvaluation;
import afg.process.afgprocess.model.procedure.Procedure;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/** Recherche multicritère des procédures. */
public final class ProcedureSpecifications {

    private ProcedureSpecifications() {
    }

    /**
     * @param idUtilisateur collaborateur connecté (critère « mon résultat »)
     * @param admin         un administrateur voit aussi les procédures inactives
     */
    public static Specification<Procedure> filtre(ProcedureFiltre f, int idUtilisateur, boolean admin) {
        Criteres<Procedure> c = Criteres.pour();
        if (admin) {
            c.booleen("actif", "ACTIVES".equals(f.getStatut()) ? Boolean.TRUE
                    : "INACTIVES".equals(f.getStatut()) ? Boolean.FALSE : null);
        } else {
            c.booleen("actif", true).booleen("direction.actif", true);
        }
        return c.contient(f.getQ(), "reference", "titre", "description")
                .egal("direction.id", f.getDirection())
                .selon(f.getResultat(), r -> resultat(r, idUtilisateur))
                .construire();
    }

    /** Résultat du collaborateur sur la procédure : ACQUIS, FORMATION_REQUISE, NON_EVALUEE, EN_COURS. */
    private static Specification<Procedure> resultat(String resultat, int idUtilisateur) {
        return (root, query, cb) -> switch (resultat) {
            case "EN_COURS" -> cb.exists(quiz(idUtilisateur, StatutEvaluation.EN_COURS, null, root, query, cb));
            case "NON_EVALUEE" -> cb.and(
                    cb.not(cb.exists(quiz(idUtilisateur, StatutEvaluation.TERMINEE, null, root, query, cb))),
                    cb.not(cb.exists(quiz(idUtilisateur, StatutEvaluation.EN_COURS, null, root, query, cb))));
            case "ACQUIS", "FORMATION_REQUISE" -> cb.exists(quiz(idUtilisateur, StatutEvaluation.TERMINEE,
                    ResultatEvaluation.valueOf(resultat), root, query, cb));
            default -> null;
        };
    }

    /**
     * Quiz du collaborateur sur la procédure dans le statut donné ; avec un résultat,
     * seul le dernier quiz terminé est considéré.
     */
    private static Subquery<Integer> quiz(int idUtilisateur, StatutEvaluation statut, ResultatEvaluation resultat,
                                          Root<Procedure> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<Integer> sq = query.subquery(Integer.class);
        Root<Evaluation> e = sq.from(Evaluation.class);
        Predicate base = cb.and(
                cb.equal(e.get("procedure"), root),
                cb.equal(e.get("utilisateur").get("id"), idUtilisateur),
                cb.equal(e.get("statut"), statut));
        if (resultat == null) {
            return sq.select(e.get("id")).where(base);
        }
        Subquery<LocalDateTime> max = query.subquery(LocalDateTime.class);
        Root<Evaluation> e2 = max.from(Evaluation.class);
        max.select(cb.greatest(e2.<LocalDateTime>get("dateFin"))).where(
                cb.equal(e2.get("procedure"), root),
                cb.equal(e2.get("utilisateur").get("id"), idUtilisateur),
                cb.equal(e2.get("statut"), StatutEvaluation.TERMINEE));
        return sq.select(e.get("id")).where(base,
                cb.equal(e.get("resultat"), resultat),
                cb.equal(e.get("dateFin"), max));
    }
}
