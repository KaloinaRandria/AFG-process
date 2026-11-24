package afg.process.afgprocess.model.utilisateur;

import lombok.Getter;

/**
 * Rôles de l'application (colonne role de la table role, sans le préfixe ROLE_).
 * La gestion des utilisateurs suit le principe des quatre yeux :
 * un IT_MAKER saisit la demande, un IT_CHECKER (autre personne) la valide.
 */
@Getter
public enum RoleApplication {
    ADMIN("Administrateur fonctionnel", "Procédures, questions, directions, suivi des évaluations"),
    IT("IT – consultation", "Consulte les utilisateurs et les demandes"),
    IT_MAKER("IT – saisie (maker)", "Saisit les demandes de création et de modification d'utilisateurs"),
    IT_CHECKER("Validation (checker)", "Valide ou rejette les demandes saisies par un autre utilisateur");

    private final String libelle;
    private final String description;

    RoleApplication(String libelle, String description) {
        this.libelle = libelle;
        this.description = description;
    }

    public static String libelleDe(String code) {
        for (RoleApplication r : values()) {
            if (r.name().equals(code)) return r.libelle;
        }
        return code;
    }
}
