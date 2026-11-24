package afg.process.afgprocess.service.procedure;

import afg.process.afgprocess.dto.ProcedureForm;
import afg.process.afgprocess.model.procedure.Procedure;
import afg.process.afgprocess.repository.procedure.ProcedureRepository;
import afg.process.afgprocess.repository.procedure.QuestionRepository;
import afg.process.afgprocess.service.exception.MetierException;
import afg.process.afgprocess.service.stockage.StockageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ProcedureService {

    private final ProcedureRepository procedureRepository;
    private final QuestionRepository questionRepository;
    private final DirectionService directionService;
    private final StockageService stockageService;

    /** Nombre de questions tirées pour chaque quiz. */
    @Value("${app.quiz.nb-questions:10}")
    private int nbQuestionsQuiz;

    public ProcedureService(ProcedureRepository procedureRepository, QuestionRepository questionRepository,
                            DirectionService directionService, StockageService stockageService) {
        this.procedureRepository = procedureRepository;
        this.questionRepository = questionRepository;
        this.directionService = directionService;
        this.stockageService = stockageService;
    }

    public int getNbQuestionsQuiz() {
        return nbQuestionsQuiz;
    }

    public List<Procedure> toutes() {
        return procedureRepository.findAllTriees();
    }

    public List<Procedure> actives() {
        return procedureRepository.findActivesTriees();
    }

    public Procedure get(Integer id) {
        return procedureRepository.findById(id).orElseThrow(() -> new MetierException("Procédure introuvable."));
    }

    /** Nombre de questions actives par procédure (id de procédure → nombre). */
    public Map<Integer, Long> nbQuestionsActives() {
        Map<Integer, Long> m = new HashMap<>();
        for (Object[] ligne : questionRepository.compterActivesParProcedure()) {
            m.put((Integer) ligne[0], (Long) ligne[1]);
        }
        return m;
    }

    public long nbQuestionsActives(Integer idProcedure) {
        return questionRepository.countByProcedureIdAndActifTrue(idProcedure);
    }

    @Transactional
    public Procedure enregistrer(ProcedureForm form, MultipartFile fichier) {
        if (form.getSeuilReussite() > nbQuestionsQuiz) {
            throw new MetierException("Le seuil de réussite ne peut pas dépasser " + nbQuestionsQuiz
                    + " (nombre de questions du quiz).");
        }
        String reference = form.getReference().trim().toUpperCase();
        procedureRepository.findByReferenceIgnoreCase(reference)
                .filter(autre -> !autre.getId().equals(form.getId()))
                .ifPresent(autre -> {
                    throw new MetierException("La référence " + reference + " existe déjà.");
                });

        boolean nouveau = form.getId() == null;
        boolean fichierRecu = fichier != null && !fichier.isEmpty();
        if (nouveau && !fichierRecu) {
            throw new MetierException("Le fichier PDF de la procédure est obligatoire.");
        }

        Procedure p = nouveau ? new Procedure() : get(form.getId());
        p.setReference(reference);
        p.setTitre(form.getTitre().trim());
        String description = form.getDescription();
        p.setDescription(description == null || description.isBlank() ? null : description.trim());
        p.setDirection(directionService.get(form.getIdDirection()));
        p.setSeuilReussite(form.getSeuilReussite());
        p.setActif(form.isActif());
        p.setDateMiseAJour(LocalDateTime.now());

        String ancienFichier = null;
        if (fichierRecu) {
            ancienFichier = p.getFichierStocke();
            p.setFichierStocke(stockageService.enregistrerPdf(fichier));
            p.setFichierNom(nettoyerNom(fichier.getOriginalFilename()));
        }
        Procedure enregistree = procedureRepository.save(p);
        stockageService.supprimer(ancienFichier);
        return enregistree;
    }

    /** Nom d'origine sans chemin ni caractère gênant pour l'en-tête Content-Disposition. */
    private static String nettoyerNom(String nom) {
        if (nom == null || nom.isBlank()) return "procedure.pdf";
        String base = nom.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        return base.replaceAll("[\\r\\n\"]", "_");
    }
}
