package afg.process.afgprocess.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Accès à l'Active Directory : authentification et recherche des utilisateurs.
 * Même fonctionnement que l'application AFG-approv-achat.
 */
@Slf4j
@Service
public class ActiveDirectory {

    private static final String[] USER_ATTRIBUTES = {
            "distinguishedName", "cn", "name", "uid",
            "sn", "givenName", "memberOf", "sAMAccountName",
            "userPrincipalName", "mail"
    };

    private final LdapConfProperties ldap;

    public ActiveDirectory(LdapConfProperties ldap) {
        this.ldap = ldap;
    }

    /**
     * Vérifie le couple identifiant / mot de passe auprès de l'AD :
     * recherche du DN avec le compte de service, puis bind avec le mot de passe de l'utilisateur.
     */
    public boolean authentify(String userName, String password) throws NamingException {
        if (password == null || password.isEmpty()) {
            // Un bind avec mot de passe vide est accepté par l'AD (bind anonyme) : on le refuse.
            return false;
        }
        LdapContext context = null;
        try {
            context = serviceContext();

            String safeUser = escapeFilter(userName);
            String filter = "(|(userPrincipalName=" + safeUser + ")(sAMAccountName=" + safeUser + ")(mail=" + safeUser + "))";

            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setCountLimit(1);

            NamingEnumeration<SearchResult> results = context.search(ldap.getEffectiveSearchBase(), filter, controls);
            if (!results.hasMore()) {
                log.info("Utilisateur non trouvé dans l'AD : {}", userName);
                return false;
            }
            String userDN = results.next().getNameInNamespace();

            // Test du mot de passe (bind utilisateur)
            LdapContext userCtx = new InitialLdapContext(env(userDN, password), null);
            userCtx.close();
            return true;
        } catch (NamingException e) {
            log.warn("Échec d'authentification AD pour {} : {}", userName, e.getMessage());
            return false;
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    /** Recherche un utilisateur par son userPrincipalName (ou mail). */
    public User getUser(String username) throws NamingException {
        LdapContext context = null;
        try {
            context = serviceContext();
            String safeUser = escapeFilter(username);
            String filter = "(|(userPrincipalName=" + safeUser + ")(mail=" + safeUser + "))";
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(USER_ATTRIBUTES);
            controls.setCountLimit(1);

            NamingEnumeration<SearchResult> results = context.search(ldap.getEffectiveSearchBase(), filter, controls);
            return results.hasMore() ? new User(results.next().getAttributes()) : null;
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    /** Liste tous les utilisateurs de l'annuaire ayant un userPrincipalName. */
    public List<User> getUsers() throws NamingException {
        List<User> users = new ArrayList<>();
        LdapContext context = null;
        try {
            context = serviceContext();
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(USER_ATTRIBUTES);

            NamingEnumeration<SearchResult> results = context.search(ldap.getEffectiveSearchBase(), "(objectClass=user)", controls);
            while (results.hasMore()) {
                Attributes attrs = results.next().getAttributes();
                if (attrs.get("userPrincipalName") != null) {
                    users.add(new User(attrs));
                }
            }
        } finally {
            if (context != null) {
                context.close();
            }
        }
        return users;
    }

    // ── Outils ───────────────────────────────────────────────────────────────

    private LdapContext serviceContext() throws NamingException {
        return new InitialLdapContext(env(ldap.getServicePrincipal(), ldap.getPassword()), null);
    }

    private Hashtable<String, String> env(String principal, String credentials) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldap.getProviderUrl());
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, credentials);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.REFERRAL, "follow"); // multi-domaines
        env.put("com.sun.jndi.ldap.connect.timeout", "5000");
        env.put("com.sun.jndi.ldap.read.timeout", "10000");
        return env;
    }

    /** Échappement des caractères spéciaux d'un filtre LDAP (RFC 4515). */
    private static String escapeFilter(String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\5c");
                case '*' -> sb.append("\\2a");
                case '(' -> sb.append("\\28");
                case ')' -> sb.append("\\29");
                case '\0' -> sb.append("\\00");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Utilisateur tel que lu dans l'Active Directory. */
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class User {
        private String distinguishedName;
        private String userPrincipal;
        private String commonName;
        private String mail;
        private String surname;
        private String firstName;
        private String samAccountName;

        public User(Attributes attr) {
            this.distinguishedName = attribute(attr, "distinguishedName");
            this.userPrincipal = attribute(attr, "userPrincipalName");
            this.commonName = attribute(attr, "cn");
            this.mail = attribute(attr, "mail");
            this.surname = attribute(attr, "sn");
            this.firstName = attribute(attr, "givenName");
            this.samAccountName = attribute(attr, "sAMAccountName");
        }

        private static String attribute(Attributes attrs, String name) {
            try {
                Attribute a = attrs.get(name);
                return a != null ? (String) a.get() : null;
            } catch (NamingException e) {
                return null;
            }
        }
    }
}
