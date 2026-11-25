package afg.process.afgprocess.controller;

import afg.process.afgprocess.dto.SuiviLigne;
import afg.process.afgprocess.controller.support.ListeTri;
import afg.process.afgprocess.dto.filtre.EvaluationFiltre;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import afg.process.afgprocess.model.evaluation.Evaluation;
import afg.process.afgprocess.model.evaluation.ResultatEvaluation;
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
        return "evaluation/quiz-saisie";
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
        return "evaluation/resultat-fiche";
    }

    // ── Listes ───────────────────────────────────────────────────────────────

    private static final ListeTri TRI_MES_EVALUATIONS = ListeTri.par(Sort.by(Sort.Direction.DESC, "dateFin"),
            "dateFin", "procedure.reference", "procedure.direction.code", "note");

    private static final ListeTri TRI_SUIVI = ListeTri.par(Sort.by(Sort.Direction.DESC, "dateFin"),
            "dateFin", "utilisateur.nom", "procedure.reference", "procedure.direction.code", "note", "resultat");

    @GetMapping("/mes-evaluations")
    public String mesEvaluations(@ModelAttribute("filtre") EvaluationFiltre filtre,
                                 @PageableDefault(size = 20) Pageable pageable,
                                 @AuthenticationPrincipal Utilisateur moi, Model model) {
        model.addAttribute("page", evaluationService.mesEvaluations(filtre, moi, TRI_MES_EVALUATIONS.appliquer(pageable)));
        model.addAttribute("directions", directionService.toutes());
        return "evaluation/mes-evaluations-liste";
    }

    /** Suivi (administrateurs) : situation actuelle par collaborateur et procédure, ou historique complet. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/suivi")
    public String suivi(@ModelAttribute("filtre") EvaluationFiltre filtre, @PageableDefault(size = 20) Pageable pageable,
                        Model model) {
        model.addAttribute("page", evaluationService.suivi(filtre, TRI_SUIVI.appliquer(pageable)));
        Map<ResultatEvaluation, Long> compteurs = evaluationService.compterParResultat(filtre);
        model.addAttribute("nbAcquis", compteurs.get(ResultatEvaluation.ACQUIS));
        model.addAttribute("nbFormationRequise", compteurs.get(ResultatEvaluation.FORMATION_REQUISE));
        model.addAttribute("directions", directionService.toutes());
        model.addAttribute("procedures", procedureService.toutes());
        return "evaluation/suivi-liste";
    }

    /** Export CSV (Excel) du suivi, avec les filtres et le tri affichés. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/suivi/export")
    public ResponseEntity<byte[]> exporterSuivi(@ModelAttribute("filtre") EvaluationFiltre filtre, Sort sort) {
        List<SuiviLigne> lignes = evaluationService.suiviComplet(filtre, TRI_SUIVI.appliquer(sort));
        DateTimeFormatter date = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        StringBuilder csv = new StringBuilder().append('﻿');   // BOM : accents corrects à l'ouverture dans Excel
        csv.append("Collaborateur;Mail;Direction du collaborateur;Procédure;Titre;Direction de la procédure;Note;Sur;Résultat;Tentatives;Date\r\n");
        for (SuiviLigne l : lignes) {
            Evaluation e = l.derniere();
            Utilisateur u = e.getUtilisateur();
            csv.append(String.join(";",
                    cellule(u.getNomComplet()), cellule(u.getMail()),
                    cellule(u.getDirection() == null ? "" : u.getDirection().getCode()),
                    cellule(e.getProcedure().getReference()), cellule(e.getProcedure().getTitre()),
                    cellule(e.getProcedure().getDirection().getCode()),
                    String.valueOf(e.getNote()), String.valueOf(e.getNbQuestions()),
                    cellule(e.getResultat().getLibelle()), String.valueOf(l.tentatives()),
                    e.getDateFin().format(date))).append("\r\n");
        }
        String nom = "suivi-" + (filtre.isHistorique() ? "historique" : "situation") + "-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nom).build().toString())
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Cellule CSV : entre guillemets si besoin ; une formule (=, +, -, @) est neutralisée. */
    private static String cellule(String valeur) {
        if (valeur == null) return "";
        String v = valeur.matches("^[=+\\-@].*") ? "'" + valeur : valeur;
        return v.matches("(?s).*[;\"\\r\\n].*") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }
}
