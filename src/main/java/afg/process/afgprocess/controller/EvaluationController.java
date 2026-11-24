package afg.process.afgprocess.controller;

import afg.process.afgprocess.dto.SuiviLigne;
import afg.process.afgprocess.model.evaluation.Evaluation;
import afg.process.afgprocess.model.utilisateur.Utilisateur;
import afg.process.afgprocess.service.evaluation.EvaluationService;
import afg.process.afgprocess.service.exception.MetierException;
import afg.process.afgprocess.service.procedure.DirectionService;
import afg.process.afgprocess.service.procedure.ProcedureService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.HtmlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Passage des quiz et suivi des résultats. */
@Controller
@RequestMapping("/evaluation")
public class EvaluationController {

    private static final String PREFIXE_REPONSE = "reponse_";

    private final EvaluationService evaluationService;
    private final ProcedureService procedureService;
    private final DirectionService directionService;

    public EvaluationController(EvaluationService evaluationService, ProcedureService procedureService,
                                DirectionService directionService) {
        this.evaluationService = evaluationService;
        this.procedureService = procedureService;
        this.directionService = directionService;
    }

    @PostMapping("/demarrer/{idProcedure:\\d+}")
    public String demarrer(@PathVariable Integer idProcedure, @AuthenticationPrincipal Utilisateur moi,
                           RedirectAttributes ra) {
        try {
            Evaluation e = evaluationService.demarrer(moi, idProcedure);
            return "redirect:/evaluation/" + e.getId();
        } catch (MetierException ex) {
            ra.addFlashAttribute("ko", HtmlUtils.htmlEscape(ex.getMessage()));
            return "redirect:/procedure/" + idProcedure;
        }
    }

    @GetMapping("/{id:\\d+}")
    public String quiz(@PathVariable Integer id, @AuthenticationPrincipal Utilisateur moi, Model model) {
        model.addAttribute("quiz", evaluationService.quiz(id, moi));
        return "evaluation/quiz";
    }

    /** Les réponses arrivent sous la forme reponse_{id de ligne}={id du choix}. */
    @PostMapping("/{id:\\d+}/soumettre")
    public String soumettre(@PathVariable Integer id, @RequestParam Map<String, String> params,
                            @AuthenticationPrincipal Utilisateur moi) {
        Map<Integer, Integer> reponses = new HashMap<>();
        params.forEach((cle, valeur) -> {
            if (cle.startsWith(PREFIXE_REPONSE)) {
                try {
                    reponses.put(Integer.valueOf(cle.substring(PREFIXE_REPONSE.length())), Integer.valueOf(valeur));
                } catch (NumberFormatException ignore) {
                    // valeur non numérique : question considérée sans réponse
                }
            }
        });
        evaluationService.soumettre(id, moi, reponses);
        return "redirect:/evaluation/" + id + "/resultat";
    }

    @GetMapping("/{id:\\d+}/resultat")
    public String resultat(@PathVariable Integer id, @AuthenticationPrincipal Utilisateur moi, Model model) {
        model.addAttribute("evaluation", evaluationService.resultat(id, moi));
        model.addAttribute("moi", moi);
        return "evaluation/resultat";
    }

    @GetMapping("/mes-evaluations")
    public String mesEvaluations(@AuthenticationPrincipal Utilisateur moi, Model model) {
        model.addAttribute("evaluations", evaluationService.historique(moi));
        return "evaluation/mes-evaluations";
    }

    /** Suivi (administrateurs) : situation actuelle par collaborateur et procédure, et historique complet. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/suivi")
    public String suivi(Model model) {
        List<Evaluation> terminees = evaluationService.toutesTerminees();
        List<SuiviLigne> situation = evaluationService.situationActuelle(terminees);
        model.addAttribute("situation", situation);
        model.addAttribute("historique", terminees);
        model.addAttribute("nbFormationRequise", situation.stream().filter(l -> l.derniere().isFormationRequise()).count());
        model.addAttribute("nbAcquis", situation.stream().filter(l -> !l.derniere().isFormationRequise()).count());
        model.addAttribute("directions", directionService.toutes());
        model.addAttribute("procedures", procedureService.toutes());
        return "evaluation/suivi";
    }
}
