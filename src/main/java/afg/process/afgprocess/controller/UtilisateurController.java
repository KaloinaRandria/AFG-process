package afg.process.afgprocess.controller;

import afg.process.afgprocess.model.utilisateur.DemandeUtilisateur;
import afg.process.afgprocess.model.utilisateur.RoleApplication;
import afg.process.afgprocess.model.utilisateur.TypeDemande;
import afg.process.afgprocess.model.utilisateur.Utilisateur;
import afg.process.afgprocess.service.exception.MetierException;
import afg.process.afgprocess.service.procedure.DirectionService;
import afg.process.afgprocess.service.utilisateur.DemandeUtilisateurService;
import afg.process.afgprocess.service.utilisateur.UtilisateurService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * Gestion des utilisateurs par l'IT, en maker-checker :
 * IT (consultation), IT_MAKER (saisie des demandes), CHECKER (validation / rejet).
 * Le mot de passe reste vérifié par l'Active Directory : seuls le mail, l'identité et les rôles sont gérés ici.
 */
@Controller
@RequestMapping("/user")
@PreAuthorize("hasAnyRole('IT', 'IT_MAKER', 'CHECKER')")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final DemandeUtilisateurService demandeService;
    private final DirectionService directionService;

    public UtilisateurController(UtilisateurService utilisateurService, DemandeUtilisateurService demandeService,
                                 DirectionService directionService) {
        this.utilisateurService = utilisateurService;
        this.demandeService = demandeService;
        this.directionService = directionService;
    }

    // ── Consultation (IT, IT_MAKER, CHECKER) ─────────────────────────────────

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("utilisateurs", utilisateurService.tous());
        model.addAttribute("enAttente", demandeService.utilisateursAvecDemandeEnAttente());
        return "user/list";
    }

    @GetMapping("/demandes")
    public String demandes(Model model) {
        model.addAttribute("enAttente", demandeService.enAttente());
        model.addAttribute("traitees", demandeService.traitees());
        return "user/demandes";
    }

    @GetMapping("/demande/{id:\\d+}")
    public String demande(@PathVariable Integer id, @AuthenticationPrincipal Utilisateur moi, Model model) {
        DemandeUtilisateur d = demandeService.get(id);
        model.addAttribute("demande", d);
        // Valeurs actuelles du compte, pour comparer avec la modification proposée
        model.addAttribute("actuel", d.getType() == TypeDemande.MODIFICATION && d.isEnAttente()
                ? utilisateurService.get(d.getUtilisateur().getId()) : null);
        model.addAttribute("roles", RoleApplication.values());
        model.addAttribute("empechement", demandeService.empechementChecker(d, moi));
        model.addAttribute("moi", moi);
        return "user/demande";
    }

    // ── Saisie (IT_MAKER) ────────────────────────────────────────────────────

    @PreAuthorize("hasRole('IT_MAKER')")
    @GetMapping("/add")
    public String add(Model model) {
        return formulaire(new Utilisateur(), model);
    }

    @PreAuthorize("hasRole('IT_MAKER')")
    @GetMapping("/{id:\\d+}/edit")
    public String edit(@PathVariable Integer id, Model model) {
        return formulaire(utilisateurService.get(id), model);
    }

    @PreAuthorize("hasRole('IT_MAKER')")
    @PostMapping("/save")
    public String save(@RequestParam(required = false) Integer id, @RequestParam String mail,
                       @RequestParam(required = false) String nom, @RequestParam(required = false) String prenom,
                       @RequestParam(required = false) Integer idDirection,
                       @RequestParam(required = false) List<String> roles,
                       @RequestParam(defaultValue = "false") boolean actif,
                       @AuthenticationPrincipal Utilisateur moi, RedirectAttributes ra) {
        try {
            DemandeUtilisateur d = demandeService.soumettre(moi, id, mail, nom, prenom, idDirection, roles, actif);
            ra.addFlashAttribute("ok", "Demande de " + d.getType().getLibelle().toLowerCase() + " pour <b>"
                    + HtmlUtils.htmlEscape(d.getMail()) + "</b> enregistrée.<br>Elle doit maintenant être validée par un CHECKER.");
            return "redirect:/user/demande/" + d.getId();
        } catch (MetierException e) {
            ra.addFlashAttribute("ko", HtmlUtils.htmlEscape(e.getMessage()));
            return id == null ? "redirect:/user/add" : "redirect:/user/" + id + "/edit";
        }
    }

    @PreAuthorize("hasRole('IT_MAKER')")
    @PostMapping("/demande/{id:\\d+}/annuler")
    public String annuler(@PathVariable Integer id, @AuthenticationPrincipal Utilisateur moi, RedirectAttributes ra) {
        demandeService.annuler(id, moi);
        ra.addFlashAttribute("ok", "Demande annulée.");
        return "redirect:/user/demandes";
    }

    // ── Validation (CHECKER) ─────────────────────────────────────────────────

    @PreAuthorize("hasRole('CHECKER')")
    @PostMapping("/demande/{id:\\d+}/valider")
    public String valider(@PathVariable Integer id, @RequestParam(required = false) String commentaire,
                          @AuthenticationPrincipal Utilisateur moi, RedirectAttributes ra) {
        try {
            Utilisateur u = demandeService.valider(id, moi, commentaire);
            ra.addFlashAttribute("ok", "Demande validée : le compte <b>" + HtmlUtils.htmlEscape(u.getMail())
                    + "</b> est à jour.");
        } catch (MetierException e) {
            ra.addFlashAttribute("ko", HtmlUtils.htmlEscape(e.getMessage()));
        }
        return "redirect:/user/demande/" + id;
    }

    @PreAuthorize("hasRole('CHECKER')")
    @PostMapping("/demande/{id:\\d+}/rejeter")
    public String rejeter(@PathVariable Integer id, @RequestParam(required = false) String motif,
                          @AuthenticationPrincipal Utilisateur moi, RedirectAttributes ra) {
        try {
            demandeService.rejeter(id, moi, motif);
            ra.addFlashAttribute("ok", "Demande rejetée.");
        } catch (MetierException e) {
            ra.addFlashAttribute("ko", HtmlUtils.htmlEscape(e.getMessage()));
        }
        return "redirect:/user/demande/" + id;
    }

    private String formulaire(Utilisateur u, Model model) {
        model.addAttribute("utilisateur", u);
        model.addAttribute("directions", directionService.toutes());
        model.addAttribute("roles", RoleApplication.values());
        return "user/form";
    }
}
