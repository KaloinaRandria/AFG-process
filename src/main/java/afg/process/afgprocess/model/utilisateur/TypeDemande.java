package afg.process.afgprocess.model.utilisateur;

import lombok.Getter;

@Getter
public enum TypeDemande {
    CREATION("Création"),
    MODIFICATION("Modification");

    private final String libelle;

    TypeDemande(String libelle) {
        this.libelle = libelle;
    }
}
