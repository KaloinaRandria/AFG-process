package afg.process.afgprocess.controller;

import afg.process.afgprocess.dto.QuestionForm;
import afg.process.afgprocess.model.procedure.Question;
import afg.process.afgprocess.service.exception.MetierException;
import afg.process.afgprocess.service.procedure.ProcedureService;
import afg.process.afgprocess.service.procedure.QuestionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/** Banque de questions d'une procédure (administrateurs). */
@Controller
@PreAuthorize("hasRole('ADMIN')")
public class QuestionController {

    private final QuestionService questionService;
    private final ProcedureService procedureService;

    public QuestionController(QuestionService questionService, ProcedureService procedureService) {
        this.questionService = questionService;
        this.procedureService = procedureService;
    }

    /** Liste des questions + formulaire d'ajout (ou de modification avec ?question=id). */
    @GetMapping("/procedure/{id:\\d+}/questions")
    public String questions(@PathVariable Integer id, @RequestParam(required = false) Integer question, Model model) {
        QuestionForm form = new QuestionForm();
        if (question != null) {
            Question q = questionService.get(question);
            if (!q.getProcedure().getId().equals(id)) {
                throw new MetierException("Cette question appartient à une autre procédure.");
            }
            form = QuestionForm.depuis(q);
        }
        return page(id, form, model);
    }

    /** Variable de chemin nommée idProcedure : un {id} serait lié au champ id du formulaire. */
    @PostMapping("/procedure/{idProcedure:\\d+}/questions/save")
    public String save(@PathVariable("idProcedure") Integer id, @ModelAttribute("form") QuestionForm form,
                       Model model, RedirectAttributes ra) {
        try {
            boolean ajout = form.getId() == null;
            questionService.enregistrer(id, form);
            ra.addFlashAttribute("ok", ajout ? "Question ajoutée." : "Question modifiée.");
            return "redirect:/procedure/" + id + "/questions";
        } catch (MetierException e) {
            model.addAttribute("ko", HtmlUtils.htmlEscape(e.getMessage()));
            return page(id, form, model);
        }
    }

    @PostMapping("/question/{id:\\d+}/basculer")
    public String basculer(@PathVariable Integer id, RedirectAttributes ra) {
        Integer idProcedure = questionService.basculerActif(id);
        return "redirect:/procedure/" + idProcedure + "/questions";
    }

    @PostMapping("/question/{id:\\d+}/supprimer")
    public String supprimer(@PathVariable Integer id, RedirectAttributes ra) {
        Integer idProcedure = questionService.idProcedureDe(id);
        ra.addFlashAttribute("ok", HtmlUtils.htmlEscape(questionService.supprimer(id)));
        return "redirect:/procedure/" + idProcedure + "/questions";
    }

    private String page(Integer idProcedure, QuestionForm form, Model model) {
        // Toujours 6 emplacements de proposition affichables (les vides sont ignorés)
        List<String> choix = form.getChoix();
        while (choix.size() < QuestionForm.NB_CHOIX_MAX) choix.add("");
        List<Question> questions = questionService.parProcedure(idProcedure);
        model.addAttribute("procedure", procedureService.get(idProcedure));
        model.addAttribute("questions", questions);
        model.addAttribute("nbActives", questions.stream().filter(Question::isActif).count());
        model.addAttribute("nbQuestionsQuiz", procedureService.getNbQuestionsQuiz());
        model.addAttribute("form", form);
        return "procedure/question-saisie-liste";
    }
}
