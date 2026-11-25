package afg.process.afgprocess.service.procedure;

import afg.process.afgprocess.model.procedure.Direction;
import afg.process.afgprocess.model.procedure.Procedure;
import afg.process.afgprocess.service.stockage.StockageService;
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
    private final StockageService stockageService;

    public DirectionService(DirectionRepository directionRepository, ProcedureRepository procedureRepository,
                            StockageService stockageService) {
        this.directionRepository = directionRepository;
        this.procedureRepository = procedureRepository;
        this.stockageService = stockageService;
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
        if (d.getCode() != null && !d.getCode().equals(codeNormalise)) {
            renommerDossier(d, codeNormalise);
        }
        d.setCode(codeNormalise);
        d.setLibelle(libelle.trim());
        d.setActif(actif);
        return directionRepository.save(d);
    }

    /** Le dossier des PDF porte le code de la direction : il est renommé avec lui, et les chemins mis à jour. */
    private void renommerDossier(Direction d, String nouveauCode) {
        String ancienDossier = stockageService.dossierDirection(d.getCode()) + "/";
        String nouveauDossier = stockageService.dossierDirection(nouveauCode) + "/";
        stockageService.renommerDossier(d.getCode(), nouveauCode);
        for (Procedure p : procedureRepository.findByDirectionId(d.getId())) {
            if (p.hasFichier() && p.getFichierStocke().startsWith(ancienDossier)) {
                p.setFichierStocke(nouveauDossier + p.getFichierStocke().substring(ancienDossier.length()));
            }
        }
    }
}
