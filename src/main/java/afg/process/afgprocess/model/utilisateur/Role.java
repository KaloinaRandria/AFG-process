package afg.process.afgprocess.model.utilisateur;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    private int id;

    /** Libellé du rôle, sans préfixe (ex. ADMIN) : Spring Security reçoit ROLE_ADMIN. */
    @Column(nullable = false, unique = true)
    private String role;

    public Role(String role) {
        this.role = role;
    }
}
