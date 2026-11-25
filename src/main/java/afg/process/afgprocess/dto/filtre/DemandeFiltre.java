package afg.process.afgprocess.dto.filtre;

import afg.process.afgprocess.model.utilisateur.StatutDemande;
import afg.process.afgprocess.model.utilisateur.TypeDemande;
import lombok.Getter;
import lombok.Setter;

/** Critères de l'historique des demandes d'utilisateurs. */
@Getter
@Setter
public class DemandeFiltre {

    /** Texte cherché dans le mail, le nom ou le prénom demandés. */
    private String q;
    private TypeDemande type;
    /** VALIDEE, REJETEE ou ANNULEE (vide = toutes les demandes traitées). */
    private StatutDemande statut;

    public boolean isActif() {
        return ProcedureFiltre.notEmpty(q) || type != null || statut != null;
    }
}
