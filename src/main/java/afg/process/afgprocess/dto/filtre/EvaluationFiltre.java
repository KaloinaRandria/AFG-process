package afg.process.afgprocess.dto.filtre;

import afg.process.afgprocess.model.evaluation.ResultatEvaluation;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Critères des listes d'évaluations (mes évaluations, suivi). */
@Getter
@Setter
public class EvaluationFiltre {

    /** Texte cherché dans la référence ou le titre de la procédure. */
    private String q;
    /** Texte cherché dans le nom, le prénom ou le mail du collaborateur. */
    private String collaborateur;
    /** Direction de la procédure. */
    private Integer direction;
    /** Direction du collaborateur. */
    private Integer directionCollaborateur;
    private Integer procedure;
    private ResultatEvaluation resultat;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate du;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate au;
    /** Suivi : « situation » (dernier quiz par collaborateur et procédure) ou « historique ». */
    private String vue = "situation";

    public boolean isActif() {
        return ProcedureFiltre.notEmpty(q) || ProcedureFiltre.notEmpty(collaborateur) || direction != null
                || directionCollaborateur != null || procedure != null || resultat != null || du != null || au != null;
    }

    public boolean isHistorique() {
        return "historique".equals(vue);
    }

    /** Copie sans le critère de résultat (compteurs par résultat). */
    public EvaluationFiltre sansResultat() {
        EvaluationFiltre f = new EvaluationFiltre();
        f.q = q; f.collaborateur = collaborateur; f.direction = direction;
        f.directionCollaborateur = directionCollaborateur; f.procedure = procedure;
        f.du = du; f.au = au; f.vue = vue;
        return f;
    }
}
