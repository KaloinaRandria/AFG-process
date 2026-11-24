package afg.process.afgprocess.service.utilisateur;

import afg.process.afgprocess.model.utilisateur.DemandeUtilisateur;
import afg.process.afgprocess.model.utilisateur.Role;
import afg.process.afgprocess.model.utilisateur.Utilisateur;
import afg.process.afgprocess.repository.utilisateur.RoleRepository;
import afg.process.afgprocess.repository.utilisateur.UtilisateurRepository;
import afg.process.afgprocess.service.exception.MetierException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;

    public UtilisateurService(UtilisateurRepository utilisateurRepository, RoleRepository roleRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
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

    /**
     * Applique une demande validée : crée le compte ou le met à jour avec les valeurs proposées.
     * Appelé uniquement par le circuit de validation (DemandeUtilisateurService).
     */
    @Transactional
    public Utilisateur appliquer(DemandeUtilisateur demande) {
        Utilisateur u = demande.getUtilisateur() != null ? get(demande.getUtilisateur().getId()) : new Utilisateur();
        utilisateurRepository.findByMailIgnoreCase(demande.getMail())
                .filter(autre -> autre.getId() != u.getId())
                .ifPresent(autre -> {
                    throw new MetierException("Un utilisateur existe déjà avec l'adresse " + demande.getMail() + ".");
                });
        u.setMail(demande.getMail());
        u.setNom(demande.getNom());
        u.setPrenom(demande.getPrenom());
        u.setDirection(demande.getDirection());
        u.setActif(demande.isActif());
        u.getRoles().clear();
        for (String code : demande.getRoles()) {
            u.getRoles().add(roleRepository.findByRole(code).orElseGet(() -> roleRepository.save(new Role(code))));
        }
        return utilisateurRepository.save(u);
    }
}
