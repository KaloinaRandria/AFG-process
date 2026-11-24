package afg.process.afgprocess.controller;

import afg.process.afgprocess.service.exception.MetierException;
import afg.process.afgprocess.service.procedure.DirectionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.HtmlUtils;

/** Gestion des directions (administrateurs). */
@Controller
@RequestMapping("/direction")
@PreAuthorize("hasRole('ADMIN')")
public class DirectionController {

    private final DirectionService directionService;

    public DirectionController(DirectionService directionService) {
        this.directionService = directionService;
    }

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("directions", directionService.toutes());
        model.addAttribute("nbProcedures", directionService.nbProceduresParDirection());
        return "direction/list";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Integer id, @RequestParam String code,
                       @RequestParam String libelle, @RequestParam(defaultValue = "false") boolean actif,
                       RedirectAttributes ra) {
        try {
            var d = directionService.enregistrer(id, code, libelle, actif);
            ra.addFlashAttribute("ok", "Direction <b>" + HtmlUtils.htmlEscape(d.getCode()) + "</b> enregistrée.");
        } catch (MetierException e) {
            ra.addFlashAttribute("ko", HtmlUtils.htmlEscape(e.getMessage()));
        }
        return "redirect:/direction/list";
    }
}
