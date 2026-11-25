package afg.process.afgprocess.configuration;

import afg.process.afgprocess.model.procedure.Procedure;
import afg.process.afgprocess.service.exception.MetierException;
import afg.process.afgprocess.service.procedure.ProcedureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Au démarrage : range dans le dossier de leur direction, sous leur nom d'origine,
 * les PDF enregistrés à l'ancienne (nom généré, à la racine du dossier des procédures).
 * Sans effet une fois tous les fichiers rangés.
 */
@Slf4j
@Component
public class RangementFichiersInitializer implements ApplicationRunner {

    private final ProcedureService procedureService;

    public RangementFichiersInitializer(ProcedureService procedureService) {
        this.procedureService = procedureService;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Procedure p : procedureService.toutes()) {
            try {
                if (procedureService.rangerFichier(p)) {
                    log.info("PDF de la procédure {} rangé : {}", p.getReference(), p.getFichierStocke());
                }
            } catch (MetierException e) {
                log.warn("PDF de la procédure {} non rangé : {}", p.getReference(), e.getMessage());
            }
        }
    }
}
