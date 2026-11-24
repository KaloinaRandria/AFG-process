package afg.process.afgprocess.controller;

import afg.process.afgprocess.model.utilisateur.Utilisateur;
import afg.process.afgprocess.service.exception.MetierException;
import afg.process.afgprocess.service.procedure.DirectionService;
import afg.process.afgprocess.service.utilisateur.UtilisateurService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.HtmlUtils;

/**
 * Comptes autorisés à se connecter (administrateurs). Le mot de passe reste vérifié
 * par l'Active Directory : on n'enregistre ici que le mail, l'identité et les droits.
 */
@Controller
@RequestMapping("/user")
@PreAuthorize("hasRole('ADMIN')")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final DirectionService directionService;

    public UtilisateurController(UtilisateurService utilisateurService, DirectionService directionService) {
        this.utilisateurService = utilisateurService;
        this.directionService = directionService;
    }

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("utilisateurs", utilisateurService.tous());
        return "user/list";
    }

    @GetMapping("/add")
    public String add(Model model) {
        return formulaire(new Utilisateur(), model);
    }

    @GetMapping("/{id:\\d+}/edit")
    public String edit(@PathVariable Integer id, Model model) {
        return formulaire(utilisateurService.get(id), model);
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Integer id, @RequestParam String mail,
                       @RequestParam(required = false) String nom, @RequestParam(required = false) String prenom,
                       @RequestParam(required = false) Integer idDirection,
                       @RequestParam(defaultValue = "false") boolean admin,
                       @RequestParam(defaultValue = "false") boolean actif,
                       @AuthenticationPrincipal Utilisateur moi, RedirectAttributes ra) {
        try {
            if (id != null && id == moi.getId() && (!admin || !actif)) {
                throw new MetierException("Vous ne pouvez pas retirer vos propres droits d'administrateur ni désactiver votre compte.");
            }
            Utilisateur u = utilisateurService.enregistrer(id, mail, nom, prenom, idDirection, admin, actif);
            ra.addFlashAttribute("ok", "Utilisateur <b>" + HtmlUtils.htmlEscape(u.getMail()) + "</b> enregistré.");
            return "redirect:/user/list";
        } catch (MetierException e) {
            ra.addFlashAttribute("ko", HtmlUtils.htmlEscape(e.getMessage()));
            return id == null ? "redirect:/user/add" : "redirect:/user/" + id + "/edit";
        }
    }

    private String formulaire(Utilisateur u, Model model) {
        model.addAttribute("utilisateur", u);
        model.addAttribute("directions", directionService.toutes());
        return "user/form";
    }
}
