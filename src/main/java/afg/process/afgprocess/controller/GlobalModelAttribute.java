package afg.process.afgprocess.controller;

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

    public GlobalModelAttribute(DemandeUtilisateurService demandeService) {
        this.demandeService = demandeService;
    }

    /** URI courante (sans le contexte) pour surligner le menu. */
    @ModelAttribute
    public void addCurrentUri(HttpServletRequest request, Model model) {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        model.addAttribute("currentUri", uri.isEmpty() ? "/" : uri);
    }

    /** Nombre de demandes d'utilisateurs à valider, affiché dans le menu des CHECKER. */
    @ModelAttribute
    public void addDemandesEnAttente(@AuthenticationPrincipal Utilisateur moi, Model model) {
        if (moi != null && moi.hasRole(RoleApplication.CHECKER)) {
            model.addAttribute("nbDemandesEnAttente", demandeService.nbEnAttente());
        }
    }
}
