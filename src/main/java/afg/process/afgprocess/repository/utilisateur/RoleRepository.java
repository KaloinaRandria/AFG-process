package afg.process.afgprocess.repository.utilisateur;

import afg.process.afgprocess.model.utilisateur.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByRole(String role);
}
