package afg.process.afgprocess.dto.filtre;

import lombok.Getter;
import lombok.Setter;

/** Critères de la liste des procédures (paramètres de l'URL). */
@Getter
@Setter
public class ProcedureFiltre {

    /** Texte cherché dans la référence, le titre ou la description. */
    private String q;
    private Integer direction;
    /** Résultat du collaborateur connecté : ACQUIS, FORMATION_REQUISE, NON_EVALUEE, EN_COURS. */
    private String resultat;
    /** Administrateurs : ACTIVES ou INACTIVES (vide = toutes). */
    private String statut;

    public boolean isActif() {
        return notEmpty(q) || direction != null || notEmpty(resultat) || notEmpty(statut);
    }

    static boolean notEmpty(String s) {
        return s != null && !s.isBlank();
    }
}
