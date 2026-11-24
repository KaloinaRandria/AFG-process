package afg.process.afgprocess.repository.procedure;

import afg.process.afgprocess.model.procedure.Direction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DirectionRepository extends JpaRepository<Direction, Integer> {

    List<Direction> findAllByOrderByCodeAsc();

    List<Direction> findByActifTrueOrderByCodeAsc();

    Optional<Direction> findByCodeIgnoreCase(String code);
}
