package afg.process.afgprocess.model.utilisateur;

import afg.process.afgprocess.model.procedure.Direction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;

/**
 * Demande de création ou de modification d'un utilisateur (maker-checker).
 * Elle contient les valeurs proposées ; le compte n'est créé ou modifié
 * qu'au moment de la validation par un CHECKER différent du maker.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "demande_utilisateur")
public class DemandeUtilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeDemande type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutDemande statut = StatutDemande.EN_ATTENTE;

    /** Compte concerné : renseigné pour une modification, et après validation pour une création. */
    @ManyToOne
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    // ── Valeurs proposées ────────────────────────────────────────────────────
    @Column(nullable = false)
    private String mail;
    private String nom;
    private String prenom;

    @ManyToOne
    @JoinColumn(name = "id_direction")
    private Direction direction;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "demande_utilisateur_role", joinColumns = @JoinColumn(name = "id_demande"))
    @Column(name = "role", length = 30)
    private Set<String> roles = new TreeSet<>();

    @Column(nullable = false)
    private boolean actif = true;

    // ── Circuit de validation ────────────────────────────────────────────────
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_maker", nullable = false)
    private Utilisateur maker;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande;

    @ManyToOne
    @JoinColumn(name = "id_checker")
    private Utilisateur checker;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    /** Motif de rejet, ou commentaire de validation. */
    @Column(length = 1000)
    private String commentaire;

    public boolean isEnAttente() {
        return statut == StatutDemande.EN_ATTENTE;
    }

    public String getNomComplet() {
        return ((prenom != null ? prenom + " " : "") + (nom != null ? nom : "")).trim();
    }
}
