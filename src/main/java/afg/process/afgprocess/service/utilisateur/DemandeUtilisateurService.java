package afg.process.afgprocess.service.utilisateur;

import afg.process.afgprocess.model.utilisateur.*;
import afg.process.afgprocess.repository.utilisateur.DemandeUtilisateurRepository;
import afg.process.afgprocess.repository.utilisateur.UtilisateurRepository;
import afg.process.afgprocess.service.exception.MetierException;
import afg.process.afgprocess.service.procedure.DirectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Circuit maker-checker de la gestion des utilisateurs :
 * l'MAKER saisit une demande, un CHECKER différent la valide (le compte est alors
 * créé ou modifié) ou la rejette avec un motif. Personne ne traite une demande
 * qui le concerne ou qu'il a saisie lui-même.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class DemandeUtilisateurService {

    private final DemandeUtilisateurRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurService utilisateurService;
    private final DirectionService directionService;

    public DemandeUtilisateurService(DemandeUtilisateurRepository demandeRepository,
                                     UtilisateurRepository utilisateurRepository,
                                     UtilisateurService utilisateurService, DirectionService directionService) {
        this.demandeRepository = demandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.utilisateurService = utilisateurService;
        this.directionService = directionService;
    }

    public List<DemandeUtilisateur> enAttente() {
        return demandeRepository.findByStatut(StatutDemande.EN_ATTENTE);
    }

    public List<DemandeUtilisateur> traitees() {
        return demandeRepository.findTraitees();
    }

    public long nbEnAttente() {
        return demandeRepository.countByStatut(StatutDemande.EN_ATTENTE);
    }

    public DemandeUtilisateur get(Integer id) {
        return demandeRepository.findById(id).orElseThrow(() -> new MetierException("Demande introuvable."));
    }

    /** Id des utilisateurs ayant une demande de modification en attente. */
    public Set<Integer> utilisateursAvecDemandeEnAttente() {
        Set<Integer> ids = new HashSet<>();
        for (DemandeUtilisateur d : enAttente()) {
            if (d.getUtilisateur() != null) ids.add(d.getUtilisateur().getId());
        }
        return ids;
    }

    // ── Maker ────────────────────────────────────────────────────────────────

    /**
     * Saisie d'une demande par un MAKER.
     * @param idUtilisateur null pour une création, l'id du compte pour une modification
     */
    @Transactional
    public DemandeUtilisateur soumettre(Utilisateur maker, Integer idUtilisateur, String mail, String nom, String prenom,
                                       Integer idDirection, Collection<String> roles, boolean actif) {
        if (mail == null || !mail.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new MetierException("Adresse mail invalide.");
        }
        String mailNormalise = mail.trim().toLowerCase();
        Set<String> codesRoles = verifierRoles(roles);

        DemandeUtilisateur d = new DemandeUtilisateur();
        if (idUtilisateur == null) {
            if (utilisateurRepository.findByMailIgnoreCase(mailNormalise).isPresent()) {
                throw new MetierException("Un utilisateur existe déjà avec l'adresse " + mailNormalise
                        + " : faites une demande de modification.");
            }
            if (demandeRepository.existsByStatutAndMailIgnoreCase(StatutDemande.EN_ATTENTE, mailNormalise)) {
                throw new MetierException("Une demande est déjà en attente de validation pour " + mailNormalise + ".");
            }
            d.setType(TypeDemande.CREATION);
        } else {
            Utilisateur cible = utilisateurService.get(idUtilisateur);
            if (cible.getId() == maker.getId()) {
                throw new MetierException("Vous ne pouvez pas saisir de demande sur votre propre compte.");
            }
            if (demandeRepository.existsByStatutAndUtilisateurId(StatutDemande.EN_ATTENTE, cible.getId())) {
                throw new MetierException("Une demande est déjà en attente de validation pour ce compte.");
            }
            utilisateurRepository.findByMailIgnoreCase(mailNormalise)
                    .filter(autre -> autre.getId() != cible.getId())
                    .ifPresent(autre -> {
                        throw new MetierException("Un autre utilisateur a déjà l'adresse " + mailNormalise + ".");
                    });
            d.setType(TypeDemande.MODIFICATION);
            d.setUtilisateur(cible);
        }

        d.setMail(mailNormalise);
        d.setNom(nom == null || nom.isBlank() ? null : nom.trim().toUpperCase());
        d.setPrenom(prenom == null || prenom.isBlank() ? null : prenom.trim());
        d.setDirection(idDirection == null ? null : directionService.get(idDirection));
        d.setRoles(codesRoles);
        d.setActif(actif);

        if (d.getType() == TypeDemande.MODIFICATION && !modifie(d.getUtilisateur(), d)) {
            throw new MetierException("Aucune modification par rapport au compte actuel.");
        }
        d.setMaker(utilisateurRepository.getReferenceById(maker.getId()));
        d.setDateDemande(LocalDateTime.now());
        d.setStatut(StatutDemande.EN_ATTENTE);
        DemandeUtilisateur enregistree = demandeRepository.save(d);
        log.info("Demande utilisateur n°{} ({} de {}) saisie par {}", enregistree.getId(), d.getType(), mailNormalise, maker.getMail());
        return enregistree;
    }

    /** Le maker retire sa propre demande tant qu'elle n'est pas traitée. */
    @Transactional
    public void annuler(Integer idDemande, Utilisateur maker) {
        DemandeUtilisateur d = demandeEnAttente(idDemande);
        if (d.getMaker().getId() != maker.getId()) {
            throw new MetierException("Seul l'auteur de la demande peut l'annuler.");
        }
        cloturer(d, StatutDemande.ANNULEE, null, null);
    }

    // ── Checker ──────────────────────────────────────────────────────────────

    @Transactional
    public Utilisateur valider(Integer idDemande, Utilisateur checker, String commentaire) {
        DemandeUtilisateur d = demandeEnAttente(idDemande);
        verifierQuatreYeux(d, checker);
        Utilisateur u = utilisateurService.appliquer(d);
        d.setUtilisateur(u);
        cloturer(d, StatutDemande.VALIDEE, checker, commentaire);
        log.info("Demande utilisateur n°{} validée par {}", d.getId(), checker.getMail());
        return u;
    }

    @Transactional
    public void rejeter(Integer idDemande, Utilisateur checker, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new MetierException("Le motif du rejet est obligatoire.");
        }
        DemandeUtilisateur d = demandeEnAttente(idDemande);
        verifierQuatreYeux(d, checker);
        cloturer(d, StatutDemande.REJETEE, checker, motif);
        log.info("Demande utilisateur n°{} rejetée par {}", d.getId(), checker.getMail());
    }

    /** Raison pour laquelle cet utilisateur ne peut pas traiter la demande, ou null s'il le peut. */
    public String empechementChecker(DemandeUtilisateur d, Utilisateur checker) {
        if (d.getMaker().getId() == checker.getId()) {
            return "Vous avez saisi cette demande : un autre CHECKER doit la valider.";
        }
        boolean concerne = (d.getUtilisateur() != null && d.getUtilisateur().getId() == checker.getId())
                || d.getMail().equalsIgnoreCase(checker.getMail());
        if (concerne) {
            return "Cette demande concerne votre propre compte : un autre CHECKER doit la valider.";
        }
        return null;
    }

    // ── Outils ───────────────────────────────────────────────────────────────

    private void verifierQuatreYeux(DemandeUtilisateur d, Utilisateur checker) {
        String empechement = empechementChecker(d, checker);
        if (empechement != null) {
            throw new MetierException(empechement);
        }
    }

    private DemandeUtilisateur demandeEnAttente(Integer id) {
        DemandeUtilisateur d = get(id);
        if (!d.isEnAttente()) {
            throw new MetierException("Cette demande a déjà été traitée (" + d.getStatut().getLibelle().toLowerCase() + ").");
        }
        return d;
    }

    private void cloturer(DemandeUtilisateur d, StatutDemande statut, Utilisateur checker, String commentaire) {
        d.setStatut(statut);
        d.setChecker(checker == null ? null : utilisateurRepository.getReferenceById(checker.getId()));
        d.setDateTraitement(LocalDateTime.now());
        d.setCommentaire(commentaire == null || commentaire.isBlank() ? null : commentaire.trim());
    }

    private static Set<String> verifierRoles(Collection<String> roles) {
        Set<String> codes = new TreeSet<>();
        if (roles == null) return codes;
        for (String code : roles) {
            try {
                codes.add(RoleApplication.valueOf(code).name());
            } catch (IllegalArgumentException e) {
                throw new MetierException("Rôle inconnu : " + code);
            }
        }
        return codes;
    }

    private static boolean modifie(Utilisateur u, DemandeUtilisateur d) {
        Integer dirActuelle = u.getDirection() == null ? null : u.getDirection().getId();
        Integer dirProposee = d.getDirection() == null ? null : d.getDirection().getId();
        return !Objects.equals(u.getMail(), d.getMail())
                || !Objects.equals(u.getNom(), d.getNom())
                || !Objects.equals(u.getPrenom(), d.getPrenom())
                || !Objects.equals(dirActuelle, dirProposee)
                || u.isActif() != d.isActif()
                || !u.getCodesRoles().equals(new TreeSet<>(d.getRoles()));
    }
}
