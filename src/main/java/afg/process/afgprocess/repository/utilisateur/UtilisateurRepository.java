package afg.process.afgprocess.repository.utilisateur;

import afg.process.afgprocess.model.utilisateur.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    Optional<Utilisateur> findByMailIgnoreCase(String mail);

    @Query("select u from Utilisateur u left join fetch u.direction order by u.nom, u.prenom")
    List<Utilisateur> findAllTries();
}
