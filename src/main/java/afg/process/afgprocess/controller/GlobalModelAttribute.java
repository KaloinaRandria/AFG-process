package afg.process.afgprocess.controller;

import afg.process.afgprocess.controller.support.ListeHelper;
import afg.process.afgprocess.model.evaluation.ResultatEvaluation;
import afg.process.afgprocess.model.utilisateur.RoleApplication;
import afg.process.afgprocess.model.utilisateur.Utilisateur;
import afg.process.afgprocess.service.utilisateur.DemandeUtilisateurService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Attributs communs à toutes les pages (menu). */
@ControllerAdvice
public class GlobalModelAttribute {

    private final DemandeUtilisateurService demandeService;

    private final ListeHelper listeHelper;

    public GlobalModelAttribute(DemandeUtilisateurService demandeService, ListeHelper listeHelper) {
        this.demandeService = demandeService;
        this.listeHelper = listeHelper;
    }

    /** URI courante (sans le contexte) pour surligner le menu. */
    @ModelAttribute
    public void addCurrentUri(HttpServletRequest request, Model model) {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        model.addAttribute("currentUri", uri.isEmpty() ? "/" : uri);
    }

    /**
     * Liens des listes paginées ({@link ListeHelper}), exposés comme variable « liste » :
     * Thymeleaf 3.1 interdit les appels de bean (@…) et la lecture de « param » dans les liens.
     */
    @ModelAttribute("liste")
    public ListeHelper liste() {
        return listeHelper;
    }

    /** Résultats possibles d'un quiz (listes déroulantes des filtres). */
    @ModelAttribute("resultats")
    public ResultatEvaluation[] resultats() {
        return ResultatEvaluation.values();
    }

    /** Nombre de demandes d'utilisateurs à valider, affiché dans le menu des CHECKER. */
    @ModelAttribute
    public void addDemandesEnAttente(@AuthenticationPrincipal Utilisateur moi, Model model) {
        if (moi != null && moi.hasRole(RoleApplication.CHECKER)) {
            model.addAttribute("nbDemandesEnAttente", demandeService.nbEnAttente());
        }
    }
}
