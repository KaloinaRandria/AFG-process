package afg.process.afgprocess.repository.utilisateur;

import afg.process.afgprocess.model.utilisateur.DemandeUtilisateur;
import afg.process.afgprocess.model.utilisateur.StatutDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DemandeUtilisateurRepository extends JpaRepository<DemandeUtilisateur, Integer>, JpaSpecificationExecutor<DemandeUtilisateur> {

    @Query("""
            select d from DemandeUtilisateur d join fetch d.maker left join fetch d.checker
            left join fetch d.utilisateur left join fetch d.direction
            where d.statut = :statut order by d.dateDemande desc""")
    List<DemandeUtilisateur> findByStatut(StatutDemande statut);

    long countByStatut(StatutDemande statut);

    boolean existsByStatutAndMailIgnoreCase(StatutDemande statut, String mail);

    boolean existsByStatutAndUtilisateurId(StatutDemande statut, int idUtilisateur);
}
