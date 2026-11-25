package afg.process.afgprocess.repository.specification;

import afg.process.afgprocess.dto.filtre.UtilisateurFiltre;
import afg.process.afgprocess.model.utilisateur.Role;
import afg.process.afgprocess.model.utilisateur.Utilisateur;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/** Recherche multicritère des utilisateurs. */
public final class UtilisateurSpecifications {

    public static final String SANS_ROLE = "COLLABORATEUR";

    private UtilisateurSpecifications() {
    }

    public static Specification<Utilisateur> filtre(UtilisateurFiltre f) {
        return Criteres.<Utilisateur>pour()
                .contient(f.getQ(), "nom", "prenom", "mail")
                .egal("direction.id", f.getDirection())
                .booleen("actif", "ACTIFS".equals(f.getStatut()) ? Boolean.TRUE
                        : "INACTIFS".equals(f.getStatut()) ? Boolean.FALSE : null)
                .selon(f.getRole(), UtilisateurSpecifications::role)
                .construire();
    }

    /** A le rôle donné (sous-requête : pas de doublon de lignes) ; COLLABORATEUR = aucun rôle. */
    private static Specification<Utilisateur> role(String code) {
        return (root, query, cb) -> {
            Subquery<Integer> sq = query.subquery(Integer.class);
            Root<Utilisateur> u = sq.from(Utilisateur.class);
            Join<Utilisateur, Role> r = u.join("roles");
            sq.select(u.get("id")).where(cb.equal(u, root));
            if (SANS_ROLE.equals(code)) {
                return cb.not(cb.exists(sq));
            }
            sq.where(cb.equal(u, root), cb.equal(r.get("role"), code));
            return cb.exists(sq);
        };
    }
}
