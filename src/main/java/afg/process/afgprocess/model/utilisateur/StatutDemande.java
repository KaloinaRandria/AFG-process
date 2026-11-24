package afg.process.afgprocess.model.utilisateur;

import lombok.Getter;

@Getter
public enum StatutDemande {
    EN_ATTENTE("En attente de validation"),
    VALIDEE("Validée"),
    REJETEE("Rejetée"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutDemande(String libelle) {
        this.libelle = libelle;
    }
}
