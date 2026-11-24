package afg.process.afgprocess.configuration;

import afg.process.afgprocess.model.utilisateur.Utilisateur;
import afg.process.afgprocess.service.utilisateur.UtilisateurService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import java.util.Collection;
import java.util.List;

/**
 * Authentification : mot de passe vérifié par l'Active Directory,
 * puis l'utilisateur doit exister (et être actif) dans la table utilisateur.
 */
@Slf4j
@Component
public class MyAuthenticationProvider implements AuthenticationProvider {

    private final ActiveDirectory activeDirectory;
    private final UtilisateurService utilisateurService;

    /** Désactivée par défaut ; activée par le profil dev (application-dev.properties). */
    @Value("${app.auth.test-login.enabled:false}")
    private boolean testLoginEnabled;

    @Value("${app.auth.test-login.password:}")
    private String testLoginPassword;

    @Value("${app.auth.test-login.comptes:}")
    private List<String> comptesTest;

    public MyAuthenticationProvider(ActiveDirectory activeDirectory, UtilisateurService utilisateurService) {
        this.activeDirectory = activeDirectory;
        this.utilisateurService = utilisateurService;
    }

    @PostConstruct
    void signalerConnexionDeTest() {
        if (testLoginEnabled) {
            log.warn("CONNEXION DE TEST ACTIVÉE pour {} compte(s) : NE PAS UTILISER EN PRODUCTION.", comptesTest.size());
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName().trim();
        String password = authentication.getCredentials().toString();

        // Connexion de test (profil dev uniquement), sans passer par l'AD
        if (estConnexionDeTest(email, password)) {
            log.info("Connexion de TEST (sans Active Directory) : {}", email);
            return authentifier(chargerUtilisateur(email));
        }

        try {
            if (!activeDirectory.authentify(email, password)) {
                throw new BadCredentialsException("Email ou mot de passe incorrect");
            }
        } catch (NamingException e) {
            throw new BadCredentialsException("Erreur de connexion à l'annuaire", e);
        }
        return authentifier(chargerUtilisateur(email));
    }

    private Utilisateur chargerUtilisateur(String email) {
        Utilisateur user = utilisateurService.getUtilisateurByMail(email);
        if (user == null) {
            throw new BadCredentialsException("Utilisateur non autorisé dans l'application");
        }
        return user;
    }

    private boolean estConnexionDeTest(String email, String password) {
        return testLoginEnabled
                && testLoginPassword != null && !testLoginPassword.isBlank()
                && password.equals(testLoginPassword)
                && comptesTest.stream().anyMatch(c -> c.trim().equalsIgnoreCase(email));
    }

    private Authentication authentifier(Utilisateur user) {
        if (!user.isActif()) {
            throw new DisabledException("Compte désactivé : contactez l'administrateur.");
        }
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.getRole()))
                .toList();
        return UsernamePasswordAuthenticationToken.authenticated(user, null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
