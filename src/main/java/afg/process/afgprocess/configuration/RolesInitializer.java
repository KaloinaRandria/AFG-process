package afg.process.afgprocess.configuration;

import afg.process.afgprocess.model.utilisateur.Role;
import afg.process.afgprocess.model.utilisateur.RoleApplication;
import afg.process.afgprocess.repository.utilisateur.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Au démarrage : crée dans la table role les rôles de l'application qui n'y sont pas encore. */
@Slf4j
@Component
public class RolesInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    public RolesInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RoleApplication r : RoleApplication.values()) {
            if (roleRepository.findByRole(r.name()).isEmpty()) {
                roleRepository.save(new Role(r.name()));
                log.info("Rôle {} créé.", r.name());
            }
        }
    }
}
