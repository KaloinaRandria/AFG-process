package afg.process.afgprocess.service.procedure;

import afg.process.afgprocess.dto.QuestionForm;
import afg.process.afgprocess.model.procedure.Choix;
import afg.process.afgprocess.model.procedure.Question;
import afg.process.afgprocess.repository.evaluation.EvaluationRepository;
import afg.process.afgprocess.repository.procedure.QuestionRepository;
import afg.process.afgprocess.service.exception.MetierException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final EvaluationRepository evaluationRepository;
    private final ProcedureService procedureService;

    public QuestionService(QuestionRepository questionRepository, EvaluationRepository evaluationRepository,
                           ProcedureService procedureService) {
        this.questionRepository = questionRepository;
        this.evaluationRepository = evaluationRepository;
        this.procedureService = procedureService;
    }

    public List<Question> parProcedure(Integer idProcedure) {
        return questionRepository.findByProcedureIdOrderByIdAsc(idProcedure);
    }

    public Question get(Integer id) {
        return questionRepository.findWithChoixById(id).orElseThrow(() -> new MetierException("Question introuvable."));
    }

    public boolean estUtilisee(Integer idQuestion) {
        return evaluationRepository.existsByReponsesQuestionId(idQuestion);
    }

    /** Crée ou modifie une question de la procédure. */
    @Transactional
    public void enregistrer(Integer idProcedure, QuestionForm form) {
        String enonce = form.getEnonce() == null ? "" : form.getEnonce().trim();
        if (enonce.isEmpty()) {
            throw new MetierException("L'énoncé de la question est obligatoire.");
        }
        if (enonce.length() > 1000) {
            throw new MetierException("L'énoncé ne doit pas dépasser 1000 caractères.");
        }

        // Propositions renseignées ; la bonne réponse est repérée par son index dans la saisie
        List<Choix> propositions = new ArrayList<>();
        boolean correcteTrouvee = false;
        List<String> saisis = form.getChoix() == null ? List.of() : form.getChoix();
        for (int i = 0; i < saisis.size() && i < QuestionForm.NB_CHOIX_MAX; i++) {
            String libelle = saisis.get(i) == null ? "" : saisis.get(i).trim();
            boolean correcte = form.getIndexCorrect() != null && form.getIndexCorrect() == i;
            if (libelle.isEmpty()) {
                if (correcte) throw new MetierException("La proposition cochée comme bonne réponse est vide.");
                continue;
            }
            if (libelle.length() > 500) {
                throw new MetierException("Une proposition ne doit pas dépasser 500 caractères.");
            }
            correcteTrouvee |= correcte;
            propositions.add(new Choix(libelle, correcte));
        }
        if (propositions.size() < 2) {
            throw new MetierException("Saisissez au moins 2 propositions.");
        }
        if (!correcteTrouvee) {
            throw new MetierException("Cochez la bonne réponse.");
        }

        Question q;
        if (form.getId() == null) {
            q = new Question();
            q.setProcedure(procedureService.get(idProcedure));
            propositions.forEach(q::ajouterChoix);
        } else {
            q = get(form.getId());
            if (!q.getProcedure().getId().equals(idProcedure)) {
                throw new MetierException("Cette question appartient à une autre procédure.");
            }
            if (choixModifies(q, propositions)) {
                if (estUtilisee(q.getId())) {
                    throw new MetierException("Cette question figure déjà dans des quiz passés : ses propositions ne peuvent "
                            + "plus être modifiées (l'historique des notes en dépend). Désactivez-la et créez-en une nouvelle.");
                }
                q.getChoix().clear();
                propositions.forEach(q::ajouterChoix);
            }
        }
        q.setEnonce(enonce);
        q.setActif(form.isActif());
        questionRepository.save(q);
    }

    private static boolean choixModifies(Question q, List<Choix> nouveaux) {
        if (q.getChoix().size() != nouveaux.size()) return true;
        for (int i = 0; i < nouveaux.size(); i++) {
            Choix a = q.getChoix().get(i);
            Choix b = nouveaux.get(i);
            if (!a.getLibelle().equals(b.getLibelle()) || a.isCorrect() != b.isCorrect()) return true;
        }
        return false;
    }

    /** Active / désactive la question ; renvoie l'id de sa procédure. */
    @Transactional
    public Integer basculerActif(Integer id) {
        Question q = get(id);
        q.setActif(!q.isActif());
        return q.getProcedure().getId();
    }

    /** Supprime la question, ou la désactive si elle figure déjà dans des quiz. Renvoie le message à afficher. */
    @Transactional
    public String supprimer(Integer id) {
        Question q = get(id);
        if (estUtilisee(id)) {
            q.setActif(false);
            return "La question figure dans des quiz déjà passés : elle a été désactivée au lieu d'être supprimée.";
        }
        questionRepository.delete(q);
        return "Question supprimée.";
    }

    public Integer idProcedureDe(Integer idQuestion) {
        return get(idQuestion).getProcedure().getId();
    }
}
