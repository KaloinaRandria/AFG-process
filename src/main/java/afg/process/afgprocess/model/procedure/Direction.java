package afg.process.afgprocess.model.procedure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Direction de la banque : chaque direction possède ses procédures. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "direction")
public class Direction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_direction")
    private Integer id;

    /** Sigle court, ex. DSI, DRH, DEX. */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private boolean actif = true;

    public String getNomAffiche() {
        return code + " – " + libelle;
    }
}
