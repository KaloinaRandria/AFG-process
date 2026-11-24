package afg.process.afgprocess.service.utilisateur;

import afg.process.afgprocess.model.utilisateur.Role;
import afg.process.afgprocess.model.utilisateur.Utilisateur;
import afg.process.afgprocess.repository.utilisateur.RoleRepository;
import afg.process.afgprocess.repository.utilisateur.UtilisateurRepository;
import afg.process.afgprocess.service.exception.MetierException;
import afg.process.afgprocess.service.procedure.DirectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UtilisateurService {

    public static final String ROLE_ADMIN = "ADMIN";

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final DirectionService directionService;

    public UtilisateurService(UtilisateurRepository utilisateurRepository, RoleRepository roleRepository,
                              DirectionService directionService) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.directionService = directionService;
    }

    public Utilisateur getUtilisateurByMail(String mail) {
        return utilisateurRepository.findByMailIgnoreCase(mail).orElse(null);
    }

    public List<Utilisateur> tous() {
        return utilisateurRepository.findAllTries();
    }

    public Utilisateur get(Integer id) {
        return utilisateurRepository.findById(id).orElseThrow(() -> new MetierException("Utilisateur introuvable."));
    }

    @Transactional
    public Utilisateur enregistrer(Integer id, String mail, String nom, String prenom, Integer idDirection,
                                   boolean admin, boolean actif) {
        if (mail == null || !mail.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new MetierException("Adresse mail invalide.");
        }
        String mailNormalise = mail.trim().toLowerCase();
        utilisateurRepository.findByMailIgnoreCase(mailNormalise)
                .filter(autre -> !Integer.valueOf(autre.getId()).equals(id))
                .ifPresent(autre -> {
                    throw new MetierException("Un utilisateur existe déjà avec l'adresse " + mailNormalise + ".");
                });

        Utilisateur u = id == null ? new Utilisateur() : get(id);
        u.setMail(mailNormalise);
        u.setNom(nom == null || nom.isBlank() ? null : nom.trim().toUpperCase());
        u.setPrenom(prenom == null || prenom.isBlank() ? null : prenom.trim());
        u.setDirection(idDirection == null ? null : directionService.get(idDirection));
        u.setActif(actif);

        Role roleAdmin = roleRepository.findByRole(ROLE_ADMIN).orElseGet(() -> roleRepository.save(new Role(ROLE_ADMIN)));
        u.getRoles().removeIf(r -> ROLE_ADMIN.equals(r.getRole()));
        if (admin) {
            u.getRoles().add(roleAdmin);
        }
        return utilisateurRepository.save(u);
    }
}
