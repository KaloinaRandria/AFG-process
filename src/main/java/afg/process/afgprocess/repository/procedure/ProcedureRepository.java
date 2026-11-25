package afg.process.afgprocess.repository.procedure;

import afg.process.afgprocess.model.procedure.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProcedureRepository extends JpaRepository<Procedure, Integer> {

    @Query("select p from Procedure p join fetch p.direction d order by d.code, p.reference")
    List<Procedure> findAllTriees();

    @Query("select p from Procedure p join fetch p.direction d where p.actif = true and d.actif = true order by d.code, p.reference")
    List<Procedure> findActivesTriees();

    Optional<Procedure> findByReferenceIgnoreCase(String reference);

    long countByDirectionId(Integer idDirection);

    List<Procedure> findByDirectionId(Integer idDirection);

    Optional<Procedure> findFirstByFichierStockeIgnoreCase(String fichierStocke);
}
