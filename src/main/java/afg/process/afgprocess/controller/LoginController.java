package afg.process.afgprocess.controller;

import afg.process.afgprocess.model.evaluation.Evaluation;
import afg.process.afgprocess.model.procedure.Procedure;
import afg.process.afgprocess.model.utilisateur.Utilisateur;
import afg.process.afgprocess.service.evaluation.EvaluationService;
import afg.process.afgprocess.service.procedure.ProcedureService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private final ProcedureService procedureService;
    private final EvaluationService evaluationService;

    public LoginController(ProcedureService procedureService, EvaluationService evaluationService) {
        this.procedureService = procedureService;
        this.evaluationService = evaluationService;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        if (request.getParameter("logout") != null) {
            model.addAttribute("infoMessage", "Vous êtes déconnecté.");
        }
        return "login";
    }

    @GetMapping("/login-error")
    public String loginError(HttpServletRequest request, Model model) {
        String message = "Email ou mot de passe incorrect";
        HttpSession session = request.getSession(false);
        if (session != null
                && session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) instanceof AuthenticationException ex) {
            message = ex.getMessage();
        }
        model.addAttribute("errorMessage", message);
        return "login";
    }

    /** Accueil : situation du collaborateur sur les procédures actives. */
    @GetMapping("/")
    public String index(@AuthenticationPrincipal Utilisateur moi, Model model) {
        List<Procedure> actives = procedureService.actives();
        Map<Integer, Evaluation> derniers = evaluationService.derniersResultats(moi);
        List<Evaluation> aRevoir = actives.stream()
                .map(p -> derniers.get(p.getId()))
                .filter(e -> e != null && e.isFormationRequise())
                .toList();
        long acquises = actives.stream()
                .map(p -> derniers.get(p.getId()))
                .filter(e -> e != null && !e.isFormationRequise())
                .count();
        model.addAttribute("nbProcedures", actives.size());
        model.addAttribute("nbAcquises", acquises);
        model.addAttribute("aRevoir", aRevoir);
        model.addAttribute("nbNonEvaluees", actives.size() - acquises - aRevoir.size());
        return "index";
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
