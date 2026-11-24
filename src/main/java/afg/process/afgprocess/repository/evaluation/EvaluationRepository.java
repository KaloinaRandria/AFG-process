package afg.process.afgprocess.repository.evaluation;

import afg.process.afgprocess.model.evaluation.Evaluation;
import afg.process.afgprocess.model.evaluation.StatutEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {

    Optional<Evaluation> findFirstByUtilisateurIdAndProcedureIdAndStatutOrderByDateDebutDesc(
            int idUtilisateur, Integer idProcedure, StatutEvaluation statut);

    @Query("""
            select e from Evaluation e join fetch e.procedure p join fetch p.direction
            where e.utilisateur.id = :idUtilisateur and e.statut = :statut
            order by e.dateFin desc""")
    List<Evaluation> findByUtilisateurAndStatut(int idUtilisateur, StatutEvaluation statut);

    @Query("""
            select e from Evaluation e join fetch e.procedure p join fetch p.direction
            join fetch e.utilisateur u left join fetch u.direction
            where e.statut = afg.process.afgprocess.model.evaluation.StatutEvaluation.TERMINEE
            order by e.dateFin desc""")
    List<Evaluation> findTerminees();

    boolean existsByReponsesQuestionId(Integer idQuestion);
}
