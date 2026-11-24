package afg.process.afgprocess.model.procedure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Procédure d'une direction. Le support est un fichier PDF stocké sur disque
 * (storage.location) ; la base ne garde que son nom.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "procedures")
public class Procedure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_procedure")
    private Integer id;

    /** Référence interne de la procédure, ex. PRO-DSI-004. */
    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @Column(nullable = false)
    private String titre;

    @Column(length = 2000)
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_direction", nullable = false)
    private Direction direction;

    /** Nom du fichier sur disque (généré). */
    @Column(name = "fichier_stocke")
    private String fichierStocke;

    /** Nom d'origine du fichier, affiché et proposé au téléchargement. */
    @Column(name = "fichier_nom")
    private String fichierNom;

    /**
     * Note minimale (nombre de bonnes réponses sur le nombre de questions du quiz)
     * pour que la procédure soit considérée comme acquise. En dessous : formation requise.
     */
    @Column(name = "seuil_reussite", nullable = false, columnDefinition = "integer not null default 7")
    private int seuilReussite = 7;

    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private boolean actif = true;

    @Column(name = "date_mise_a_jour")
    private LocalDateTime dateMiseAJour;

    public boolean hasFichier() {
        return fichierStocke != null && !fichierStocke.isBlank();
    }
}
