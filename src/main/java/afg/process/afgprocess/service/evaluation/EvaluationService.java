package afg.process.afgprocess.service.evaluation;

import afg.process.afgprocess.dto.QuizVue;
import afg.process.afgprocess.dto.SuiviLigne;
import afg.process.afgprocess.model.evaluation.Evaluation;
import afg.process.afgprocess.model.evaluation.EvaluationReponse;
import afg.process.afgprocess.model.evaluation.ResultatEvaluation;
import afg.process.afgprocess.model.evaluation.StatutEvaluation;
import afg.process.afgprocess.model.procedure.Choix;
import afg.process.afgprocess.model.procedure.Procedure;
import afg.process.afgprocess.model.procedure.Question;
import afg.process.afgprocess.model.utilisateur.Utilisateur;
import afg.process.afgprocess.repository.evaluation.EvaluationRepository;
import afg.process.afgprocess.repository.procedure.QuestionRepository;
import afg.process.afgprocess.repository.utilisateur.UtilisateurRepository;
import afg.process.afgprocess.service.exception.MetierException;
import afg.process.afgprocess.service.procedure.ProcedureService;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Déroulement d'un quiz : tirage des questions, notation, décision (acquis / formation requise).
 */
@Service
@Transactional(readOnly = true)
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final QuestionRepository questionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProcedureService procedureService;

    public EvaluationService(EvaluationRepository evaluationRepository, QuestionRepository questionRepository,
                             UtilisateurRepository utilisateurRepository, ProcedureService procedureService) {
        this.evaluationRepository = evaluationRepository;
        this.questionRepository = questionRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.procedureService = procedureService;
    }

    /**
     * Démarre un quiz sur la procédure : reprend le quiz en cours s'il y en a un
     * (pas de nouveau tirage en rechargeant la page), sinon tire les questions au hasard.
     */
    @Transactional
    public Evaluation demarrer(Utilisateur utilisateur, Integer idProcedure) {
        Optional<Evaluation> enCours = evaluationRepository
                .findFirstByUtilisateurIdAndProcedureIdAndStatutOrderByDateDebutDesc(
                        utilisateur.getId(), idProcedure, StatutEvaluation.EN_COURS);
        if (enCours.isPresent()) {
            return enCours.get();
        }

        Procedure procedure = procedureService.get(idProcedure);
        if (!procedure.isActif() || !procedure.getDirection().isActif()) {
            throw new MetierException("Cette procédure n'est plus active.");
        }
        int nb = procedureService.getNbQuestionsQuiz();
        List<Integer> ids = new ArrayList<>(questionRepository.findIdsActives(idProcedure));
        if (ids.size() < nb) {
            throw new MetierException("Le quiz de cette procédure n'est pas encore disponible ("
                    + ids.size() + " question(s) sur " + nb + " nécessaires).");
        }
        Collections.shuffle(ids);

        Evaluation e = new Evaluation();
        e.setUtilisateur(utilisateurRepository.getReferenceById(utilisateur.getId()));
        e.setProcedure(procedure);
        e.setDateDebut(LocalDateTime.now());
        e.setStatut(StatutEvaluation.EN_COURS);
        e.setNbQuestions(nb);
        e.setSeuil(procedure.getSeuilReussite());
        int ordre = 1;
        for (Integer idQuestion : ids.subList(0, nb)) {
            EvaluationReponse r = new EvaluationReponse();
            r.setEvaluation(e);
            r.setQuestion(questionRepository.getReferenceById(idQuestion));
            r.setOrdre(ordre++);
            e.getReponses().add(r);
        }
        return evaluationRepository.save(e);
    }

    /** Quiz à afficher (propositions mélangées, sans indication des bonnes réponses). */
    public QuizVue quiz(Integer idEvaluation, Utilisateur utilisateur) {
        Evaluation e = getPourUtilisateur(idEvaluation, utilisateur, false);
        if (e.isTerminee()) {
            throw new MetierException("Ce quiz est déjà terminé.");
        }
        List<QuizVue.QuestionQuiz> questions = new ArrayList<>();
        for (EvaluationReponse r : e.getReponses()) {
            List<QuizVue.ChoixQuiz> choix = new ArrayList<>();
            for (Choix c : r.getQuestion().getChoix()) {
                choix.add(new QuizVue.ChoixQuiz(c.getId(), c.getLibelle()));
            }
            Collections.shuffle(choix);
            questions.add(new QuizVue.QuestionQuiz(r.getId(), r.getOrdre(), r.getQuestion().getEnonce(), choix));
        }
        Procedure p = e.getProcedure();
        return new QuizVue(e.getId(), p.getTitre(), p.getReference(), p.getDirection().getNomAffiche(),
                e.getSeuil(), questions);
    }

    /**
     * Note le quiz : une bonne réponse = 1 point. Note ≥ seuil → acquis, sinon formation requise.
     * @param reponses id de la ligne EvaluationReponse → id du choix sélectionné
     */
    @Transactional
    public Evaluation soumettre(Integer idEvaluation, Utilisateur utilisateur, Map<Integer, Integer> reponses) {
        Evaluation e = getPourUtilisateur(idEvaluation, utilisateur, false);
        if (e.isTerminee()) {
            throw new MetierException("Ce quiz a déjà été soumis.");
        }
        int note = 0;
        for (EvaluationReponse r : e.getReponses()) {
            Integer idChoix = reponses.get(r.getId());
            Choix choisi = idChoix == null ? null : r.getQuestion().getChoix().stream()
                    .filter(c -> c.getId().equals(idChoix)).findFirst().orElse(null);
            boolean correct = choisi != null && choisi.isCorrect();
            r.setChoix(choisi);
            r.setCorrect(correct);
            if (correct) note++;
        }
        e.setNote(note);
        e.setResultat(note >= e.getSeuil() ? ResultatEvaluation.ACQUIS : ResultatEvaluation.FORMATION_REQUISE);
        e.setStatut(StatutEvaluation.TERMINEE);
        e.setDateFin(LocalDateTime.now());
        return e;
    }

    /** Évaluation terminée avec ses réponses (le collaborateur concerné ou un administrateur). */
    public Evaluation resultat(Integer idEvaluation, Utilisateur utilisateur) {
        Evaluation e = getPourUtilisateur(idEvaluation, utilisateur, true);
        if (!e.isTerminee()) {
            throw new MetierException("Ce quiz n'est pas encore terminé.");
        }
        return e;
    }

    /** Quiz terminés du collaborateur, du plus récent au plus ancien. */
    public List<Evaluation> historique(Utilisateur utilisateur) {
        return evaluationRepository.findByUtilisateurAndStatut(utilisateur.getId(), StatutEvaluation.TERMINEE);
    }

    /** Dernier quiz terminé du collaborateur pour chaque procédure (id de procédure → évaluation). */
    public Map<Integer, Evaluation> derniersResultats(Utilisateur utilisateur) {
        Map<Integer, Evaluation> m = new HashMap<>();
        for (Evaluation e : historique(utilisateur)) {
            m.putIfAbsent(e.getProcedure().getId(), e);
        }
        return m;
    }

    /** Id des procédures pour lesquelles le collaborateur a un quiz commencé mais non soumis. */
    public Set<Integer> proceduresEnCours(Utilisateur utilisateur) {
        Set<Integer> ids = new HashSet<>();
        for (Evaluation e : evaluationRepository.findByUtilisateurAndStatut(utilisateur.getId(), StatutEvaluation.EN_COURS)) {
            ids.add(e.getProcedure().getId());
        }
        return ids;
    }

    /** Toutes les évaluations terminées (historique complet, pour le suivi). */
    public List<Evaluation> toutesTerminees() {
        return evaluationRepository.findTerminees();
    }

    /** Situation actuelle : dernier résultat de chaque collaborateur sur chaque procédure. */
    public List<SuiviLigne> situationActuelle(List<Evaluation> terminees) {
        Map<String, Evaluation> derniere = new LinkedHashMap<>();
        Map<String, Long> tentatives = new HashMap<>();
        for (Evaluation e : terminees) {   // triées de la plus récente à la plus ancienne
            String cle = e.getUtilisateur().getId() + "-" + e.getProcedure().getId();
            derniere.putIfAbsent(cle, e);
            tentatives.merge(cle, 1L, Long::sum);
        }
        return derniere.entrySet().stream()
                .map(en -> new SuiviLigne(en.getValue(), tentatives.get(en.getKey())))
                .toList();
    }

    private Evaluation getPourUtilisateur(Integer id, Utilisateur utilisateur, boolean adminAutorise) {
        Evaluation e = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean proprietaire = e.getUtilisateur().getId() == utilisateur.getId();
        if (!proprietaire && !(adminAutorise && utilisateur.isAdmin())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        // Chargement des réponses, questions et propositions (open-in-view désactivé)
        for (EvaluationReponse r : e.getReponses()) {
            Hibernate.initialize(r.getQuestion().getChoix());
        }
        return e;
    }
}
