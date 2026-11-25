package afg.process.afgprocess.dto.filtre;

import lombok.Getter;
import lombok.Setter;

/** Critères de la liste des utilisateurs. */
@Getter
@Setter
public class UtilisateurFiltre {

    /** Texte cherché dans le nom, le prénom ou le mail. */
    private String q;
    private Integer direction;
    /** Code de rôle, ou COLLABORATEUR pour les comptes sans rôle. */
    private String role;
    /** ACTIFS ou INACTIFS (vide = tous). */
    private String statut;

    public boolean isActif() {
        return ProcedureFiltre.notEmpty(q) || direction != null || ProcedureFiltre.notEmpty(role)
                || ProcedureFiltre.notEmpty(statut);
    }
}
