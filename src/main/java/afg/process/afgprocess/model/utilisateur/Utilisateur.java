package afg.process.afgprocess.model.utilisateur;

import afg.process.afgprocess.model.procedure.Direction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Utilisateur autorisé dans l'application. Le mot de passe n'est pas stocké :
 * il est vérifié par l'Active Directory, la base ne sert qu'aux habilitations.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilisateur")
    private int id;

    private String nom;
    private String prenom;

    @Column(nullable = false, unique = true)
    private String mail;

    /** Direction de rattachement du collaborateur (facultative : utile pour le suivi). */
    @ManyToOne
    @JoinColumn(name = "id_direction")
    private Direction direction;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "utilisateur_role",
            joinColumns = @JoinColumn(name = "id_utilisateur"),
            inverseJoinColumns = @JoinColumn(name = "id_role")
    )
    private Set<Role> roles = new HashSet<>();

    /** Un collaborateur parti est désactivé (jamais supprimé) : il ne peut plus se connecter. */
    @Column(name = "actif", nullable = false, columnDefinition = "boolean not null default true")
    private boolean actif = true;

    public boolean hasRole(RoleApplication role) {
        return roles.stream().anyMatch(r -> role.name().equals(r.getRole()));
    }

    public boolean isAdmin() {
        return hasRole(RoleApplication.ADMIN);
    }

    /** Codes des rôles, triés (ex. [ADMIN, IT]). */
    public Set<String> getCodesRoles() {
        return roles.stream().map(Role::getRole).collect(Collectors.toCollection(TreeSet::new));
    }

    public String getNomComplet() {
        return ((prenom != null ? prenom + " " : "") + (nom != null ? nom : "")).trim();
    }
}
