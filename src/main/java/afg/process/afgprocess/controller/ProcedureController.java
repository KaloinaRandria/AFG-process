package afg.process.afgprocess.controller;

import afg.process.afgprocess.dto.ProcedureForm;
import afg.process.afgprocess.model.evaluation.Evaluation;
import afg.process.afgprocess.model.procedure.Procedure;
import afg.process.afgprocess.model.utilisateur.Utilisateur;
import afg.process.afgprocess.service.evaluation.EvaluationService;
import afg.process.afgprocess.service.exception.MetierException;
import afg.process.afgprocess.service.procedure.DirectionService;
import afg.process.afgprocess.service.procedure.ProcedureService;
import afg.process.afgprocess.service.stockage.StockageService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/procedure")
public class ProcedureController {

    private final ProcedureService procedureService;
    private final DirectionService directionService;
    private final EvaluationService evaluationService;
    private final StockageService stockageService;

    public ProcedureController(ProcedureService procedureService, DirectionService directionService,
                               EvaluationService evaluationService, StockageService stockageService) {
        this.procedureService = procedureService;
        this.directionService = directionService;
        this.evaluationService = evaluationService;
        this.stockageService = stockageService;
    }

    /** Catalogue des procédures, avec le dernier résultat du collaborateur connecté. */
    @GetMapping("/list")
    public String list(@AuthenticationPrincipal Utilisateur moi, Model model) {
        List<Procedure> procedures = moi.isAdmin() ? procedureService.toutes() : procedureService.actives();
        model.addAttribute("procedures", procedures);
        model.addAttribute("directions", directionService.actives());
        model.addAttribute("nbQuestions", procedureService.nbQuestionsActives());
        model.addAttribute("nbQuestionsQuiz", procedureService.getNbQuestionsQuiz());
        model.addAttribute("derniers", evaluationService.derniersResultats(moi));
        model.addAttribute("enCours", evaluationService.proceduresEnCours(moi));
        return "procedure/list";
    }

    /** Fiche de la procédure : lecture du PDF (support de formation) et accès au quiz. */
    @GetMapping("/{id:\\d+}")
    public String voir(@PathVariable Integer id, @AuthenticationPrincipal Utilisateur moi, Model model) {
        Procedure p = procedureService.get(id);
        if (!moi.isAdmin() && (!p.isActif() || !p.getDirection().isActif())) {
            throw new MetierException("Cette procédure n'est plus active.");
        }
        Evaluation derniere = evaluationService.derniersResultats(moi).get(id);
        model.addAttribute("procedure", p);
        model.addAttribute("derniere", derniere);
        model.addAttribute("enCours", evaluationService.proceduresEnCours(moi).contains(id));
        model.addAttribute("nbQuestionsActives", procedureService.nbQuestionsActives(id));
        model.addAttribute("nbQuestionsQuiz", procedureService.getNbQuestionsQuiz());
        return "procedure/voir";
    }

    /** Fichier PDF : affiché dans le navigateur, ou téléchargé avec ?telecharger. */
    @GetMapping("/{id:\\d+}/pdf")
    public ResponseEntity<Resource> pdf(@PathVariable Integer id,
                                        @RequestParam(required = false) String telecharger,
                                        @AuthenticationPrincipal Utilisateur moi) {
        Procedure p = procedureService.get(id);
        if (!p.hasFichier() || (!moi.isAdmin() && !p.isActif())) {
            throw new MetierException("Aucun fichier disponible pour cette procédure.");
        }
        ContentDisposition disposition = (telecharger != null ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(p.getFichierNom(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(stockageService.charger(p.getFichierStocke()));
    }

    // ── Administration ───────────────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/add")
    public String add(Model model) {
        return formulaire(new ProcedureForm(), model);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id:\\d+}/edit")
    public String edit(@PathVariable Integer id, Model model) {
        return formulaire(ProcedureForm.depuis(procedureService.get(id)), model);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") ProcedureForm form, BindingResult erreurs,
                       @RequestParam(value = "fichier", required = false) MultipartFile fichier,
                       Model model, RedirectAttributes ra) {
        if (erreurs.hasErrors()) {
            model.addAttribute("ko", HtmlUtils.htmlEscape(erreurs.getAllErrors().getFirst().getDefaultMessage()));
            return formulaire(form, model);
        }
        try {
            Procedure p = procedureService.enregistrer(form, fichier);
            ra.addFlashAttribute("ok", "Procédure <b>" + HtmlUtils.htmlEscape(p.getReference()) + "</b> enregistrée.");
            return "redirect:/procedure/" + p.getId() + "/questions";
        } catch (MetierException e) {
            model.addAttribute("ko", HtmlUtils.htmlEscape(e.getMessage()));
            return formulaire(form, model);
        }
    }

    private String formulaire(ProcedureForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("directions", directionService.toutes());
        model.addAttribute("nbQuestionsQuiz", procedureService.getNbQuestionsQuiz());
        return "procedure/form";
    }
}
