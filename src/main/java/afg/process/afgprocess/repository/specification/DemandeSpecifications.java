package afg.process.afgprocess.repository.specification;

import afg.process.afgprocess.dto.filtre.DemandeFiltre;
import afg.process.afgprocess.model.utilisateur.DemandeUtilisateur;
import afg.process.afgprocess.model.utilisateur.StatutDemande;
import org.springframework.data.jpa.domain.Specification;

/** Recherche multicritère dans l'historique des demandes d'utilisateurs (demandes traitées). */
public final class DemandeSpecifications {

    private DemandeSpecifications() {
    }

    public static Specification<DemandeUtilisateur> historique(DemandeFiltre f) {
        return Criteres.<DemandeUtilisateur>pour()
                .et((root, query, cb) -> cb.notEqual(root.get("statut"), StatutDemande.EN_ATTENTE))
                .egal("statut", f.getStatut())
                .egal("type", f.getType())
                .contient(f.getQ(), "mail", "nom", "prenom")
                .construire();
    }
}
