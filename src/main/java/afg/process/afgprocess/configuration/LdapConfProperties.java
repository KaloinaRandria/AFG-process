package afg.process.afgprocess.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Paramètres de connexion à l'Active Directory (préfixe "ldap." dans application.properties).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ldap")
public class LdapConfProperties {

    /** Compte de service utilisé pour rechercher les utilisateurs (ex. intranet.mada). */
    private String username;

    /** Mot de passe du compte de service (application-secret.properties). */
    private String password;

    /** Domaine AD (ex. afgbank.com). */
    private String domainName;

    /** Adresse du contrôleur de domaine (ex. 10.25.10.10). */
    private String serverName;

    /** Base de recherche, ex. DC=afgbank,DC=com (déduite du domaine si vide). */
    private String searchBase;

    public String getProviderUrl() {
        return "ldap://" + serverName;
    }

    public String getServicePrincipal() {
        return username + "@" + domainName;
    }

    public String getEffectiveSearchBase() {
        if (searchBase != null && !searchBase.isBlank()) {
            return searchBase;
        }
        StringBuilder buf = new StringBuilder();
        for (String token : domainName.split("\\.")) {
            if (token.isEmpty()) continue;
            if (!buf.isEmpty()) buf.append(",");
            buf.append("DC=").append(token);
        }
        return buf.toString();
    }
}
