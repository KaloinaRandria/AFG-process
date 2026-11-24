package afg.process.afgprocess.service.procedure;

import afg.process.afgprocess.model.procedure.Direction;
import afg.process.afgprocess.repository.procedure.DirectionRepository;
import afg.process.afgprocess.repository.procedure.ProcedureRepository;
import afg.process.afgprocess.service.exception.MetierException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DirectionService {

    private final DirectionRepository directionRepository;
    private final ProcedureRepository procedureRepository;

    public DirectionService(DirectionRepository directionRepository, ProcedureRepository procedureRepository) {
        this.directionRepository = directionRepository;
        this.procedureRepository = procedureRepository;
    }

    public List<Direction> toutes() {
        return directionRepository.findAllByOrderByCodeAsc();
    }

    public List<Direction> actives() {
        return directionRepository.findByActifTrueOrderByCodeAsc();
    }

    public Direction get(Integer id) {
        return directionRepository.findById(id).orElseThrow(() -> new MetierException("Direction introuvable."));
    }

    public Map<Integer, Long> nbProceduresParDirection() {
        return toutes().stream().collect(Collectors.toMap(Direction::getId,
                d -> procedureRepository.countByDirectionId(d.getId())));
    }

    @Transactional
    public Direction enregistrer(Integer id, String code, String libelle, boolean actif) {
        if (code == null || code.isBlank() || libelle == null || libelle.isBlank()) {
            throw new MetierException("Le code et le libellé sont obligatoires.");
        }
        String codeNormalise = code.trim().toUpperCase();
        if (codeNormalise.length() > 20) {
            throw new MetierException("Le code ne doit pas dépasser 20 caractères.");
        }
        directionRepository.findByCodeIgnoreCase(codeNormalise)
                .filter(autre -> !autre.getId().equals(id))
                .ifPresent(autre -> {
                    throw new MetierException("Le code " + codeNormalise + " existe déjà.");
                });

        Direction d = id == null ? new Direction() : get(id);
        d.setCode(codeNormalise);
        d.setLibelle(libelle.trim());
        d.setActif(actif);
        return directionRepository.save(d);
    }
}
