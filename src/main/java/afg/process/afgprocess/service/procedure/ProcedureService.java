package afg.process.afgprocess.service.procedure;

import afg.process.afgprocess.dto.ProcedureForm;
import afg.process.afgprocess.dto.filtre.ProcedureFiltre;
import afg.process.afgprocess.model.utilisateur.Utilisateur;
import afg.process.afgprocess.repository.specification.ProcedureSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Recherche multicritère paginée (un collaborateur ne voit que les procédures actives). */
    public Page<Procedure> rechercher(ProcedureFiltre filtre, Utilisateur moi, Pageable pageable) {
        return procedureRepository.findAll(ProcedureSpecifications.filtre(filtre, moi.getId(), moi.isAdmin()), pageable);
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

        // Fichier rangé dans le dossier de la direction, sous son nom d'origine
        String ancienFichier = p.getFichierStocke();
        String codeDirection = p.getDirection().getCode();
        if (fichierRecu) {
            String chemin = stockageService.cheminRelatif(codeDirection, fichier.getOriginalFilename());
            verifierNomLibre(chemin, p.getId());
            stockageService.enregistrerPdf(fichier, chemin);
            p.setFichierStocke(chemin);
        } else if (p.hasFichier()) {
            // Changement de direction : le fichier suit dans le dossier de la nouvelle direction
            String chemin = stockageService.cheminRelatif(codeDirection, p.getFichierNom());
            if (!chemin.equalsIgnoreCase(ancienFichier)) {
                verifierNomLibre(chemin, p.getId());
                stockageService.deplacer(ancienFichier, chemin);
                p.setFichierStocke(chemin);
            }
        }
        if (p.hasFichier()) {
            p.setFichierNom(nomDuFichier(p.getFichierStocke()));
        }
        Procedure enregistree = procedureRepository.save(p);
        if (fichierRecu && ancienFichier != null && !ancienFichier.equalsIgnoreCase(p.getFichierStocke())) {
            stockageService.supprimer(ancienFichier);
        }
        return enregistree;
    }

    /**
     * Range dans le dossier de sa direction, sous son nom d'origine, un PDF encore stocké
     * à l'ancienne (nom généré à la racine). Renvoie true si le fichier a été déplacé.
     */
    @Transactional
    public boolean rangerFichier(Procedure p) {
        if (!p.hasFichier() || p.getFichierStocke().contains("/")) return false;
        String chemin = stockageService.cheminRelatif(p.getDirection().getCode(), p.getFichierNom());
        if (procedureRepository.findFirstByFichierStockeIgnoreCase(chemin).isPresent()) {
            // Nom déjà pris par une autre procédure du même dossier : on le distingue par la référence
            chemin = stockageService.cheminRelatif(p.getDirection().getCode(),
                    p.getReference() + " - " + p.getFichierNom());
        }
        stockageService.deplacer(p.getFichierStocke(), chemin);
        p.setFichierStocke(chemin);
        p.setFichierNom(nomDuFichier(chemin));
        procedureRepository.save(p);
        return true;
    }

    /** Refuse un nom de fichier déjà utilisé par une autre procédure dans le même dossier. */
    private void verifierNomLibre(String chemin, Integer idProcedure) {
        procedureRepository.findFirstByFichierStockeIgnoreCase(chemin)
                .filter(autre -> !autre.getId().equals(idProcedure))
                .ifPresent(autre -> {
                    throw new MetierException("Le fichier « " + nomDuFichier(chemin) + " » existe déjà pour la procédure "
                            + autre.getReference() + " dans le dossier " + autre.getDirection().getCode()
                            + ". Renommez votre fichier avant de l'envoyer.");
                });
    }

    private static String nomDuFichier(String cheminRelatif) {
        return cheminRelatif.substring(cheminRelatif.lastIndexOf('/') + 1);
    }
}
