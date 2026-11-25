package afg.process.afgprocess.repository.evaluation;

import afg.process.afgprocess.model.evaluation.Evaluation;
import afg.process.afgprocess.model.evaluation.StatutEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Integer>, JpaSpecificationExecutor<Evaluation> {

    Optional<Evaluation> findFirstByUtilisateurIdAndProcedureIdAndStatutOrderByDateDebutDesc(
            int idUtilisateur, Integer idProcedure, StatutEvaluation statut);

    @Query("""
            select e from Evaluation e join fetch e.procedure p join fetch p.direction
            where e.utilisateur.id = :idUtilisateur and e.statut = :statut
            order by e.dateFin desc""")
    List<Evaluation> findByUtilisateurAndStatut(int idUtilisateur, StatutEvaluation statut);

    boolean existsByReponsesQuestionId(Integer idQuestion);

    /** Nombre de quiz terminés par couple [idUtilisateur, idProcedure, nombre]. */
    @Query("""
            select e.utilisateur.id, e.procedure.id, count(e) from Evaluation e
            where e.statut = afg.process.afgprocess.model.evaluation.StatutEvaluation.TERMINEE
              and e.utilisateur.id in :idsUtilisateurs and e.procedure.id in :idsProcedures
            group by e.utilisateur.id, e.procedure.id""")
    List<Object[]> compterTentatives(Collection<Integer> idsUtilisateurs, Collection<Integer> idsProcedures);
}
