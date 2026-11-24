package afg.process.afgprocess.repository.procedure;

import afg.process.afgprocess.model.procedure.Question;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Integer> {

    @EntityGraph(attributePaths = {"choix"})
    List<Question> findByProcedureIdOrderByIdAsc(Integer idProcedure);

    @EntityGraph(attributePaths = {"choix", "procedure"})
    Optional<Question> findWithChoixById(Integer id);

    @Query("select q.id from Question q where q.procedure.id = :idProcedure and q.actif = true")
    List<Integer> findIdsActives(Integer idProcedure);

    long countByProcedureIdAndActifTrue(Integer idProcedure);

    /** Nombre de questions actives par procédure : [idProcedure, nombre]. */
    @Query("select q.procedure.id, count(q) from Question q where q.actif = true group by q.procedure.id")
    List<Object[]> compterActivesParProcedure();
}
