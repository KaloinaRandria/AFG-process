package afg.process.afgprocess.repository.specification;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/**
 * Construction générique d'une recherche multicritère (Spring Data JPA {@link Specification}).
 * Un critère dont la valeur est vide est ignoré : le formulaire de filtres peut être passé tel quel.
 * Les chemins acceptent les associations (« direction.code », « utilisateur.nom »), jointes en LEFT JOIN.
 *
 * <pre>
 * Specification&lt;Procedure&gt; spec = Criteres.&lt;Procedure&gt;pour()
 *         .contient(filtre.getQ(), "reference", "titre")
 *         .egal("direction.id", filtre.getDirection())
 *         .entre("dateMiseAJour", filtre.getDu(), filtre.getAu())
 *         .si(filtre.isX(), (root, query, cb) -&gt; ...)     // critère particulier (sous-requête…)
 *         .construire();
 * </pre>
 */
public final class Criteres<T> {

    /** Un critère : construit son prédicat à partir du contexte (racine, jointures, CriteriaBuilder). */
    @FunctionalInterface
    private interface Critere<T> {
        Predicate predicat(Contexte<T> ctx);
    }

    private final List<Critere<T>> criteres = new ArrayList<>();

    private Criteres() {
    }

    public static <T> Criteres<T> pour() {
        return new Criteres<>();
    }

    /** Texte contenu (sans tenir compte de la casse) dans au moins un des champs. */
    public Criteres<T> contient(String texte, String... chemins) {
        if (!renseigne(texte)) return this;
        String motif = "%" + texte.trim().toLowerCase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        criteres.add(ctx -> ctx.cb.or(Arrays.stream(chemins)
                .map(c -> ctx.cb.like(ctx.cb.lower(ctx.<String>chemin(c)), motif, '\\'))
                .toArray(Predicate[]::new)));
        return this;
    }

    /** Égalité (ignorée si la valeur est null ou une chaîne vide). */
    public Criteres<T> egal(String chemin, Object valeur) {
        if (valeur == null || (valeur instanceof String s && s.isBlank())) return this;
        criteres.add(ctx -> ctx.cb.equal(ctx.chemin(chemin), valeur));
        return this;
    }

    /** Booléen vrai ou faux (ignoré si null). */
    public Criteres<T> booleen(String chemin, Boolean valeur) {
        if (valeur == null) return this;
        criteres.add(ctx -> valeur ? ctx.cb.isTrue(ctx.chemin(chemin)) : ctx.cb.isFalse(ctx.chemin(chemin)));
        return this;
    }

    /** Date-heure comprise entre deux jours inclus (chaque borne est facultative). */
    public Criteres<T> entre(String chemin, LocalDate du, LocalDate au) {
        if (du != null) {
            criteres.add(ctx -> ctx.cb.greaterThanOrEqualTo(ctx.chemin(chemin), du.atStartOfDay()));
        }
        if (au != null) {
            criteres.add(ctx -> ctx.cb.lessThan(ctx.chemin(chemin), au.plusDays(1).atStartOfDay()));
        }
        return this;
    }

    /** Critère particulier (sous-requête, condition composée…), ajouté seulement si la condition est vraie. */
    public Criteres<T> si(boolean condition, Specification<T> specification) {
        if (condition) {
            criteres.add(ctx -> specification.toPredicate(ctx.root, ctx.query, ctx.cb));
        }
        return this;
    }

    /** Critère particulier toujours appliqué. */
    public Criteres<T> et(Specification<T> specification) {
        return si(true, specification);
    }

    /** Critère particulier dépendant d'une valeur (ignoré si la valeur est vide). */
    public <V> Criteres<T> selon(V valeur, Function<V, Specification<T>> specification) {
        boolean vide = valeur == null || (valeur instanceof String s && s.isBlank());
        return si(!vide, vide ? null : specification.apply(valeur));
    }

    public Specification<T> construire() {
        List<Critere<T>> liste = List.copyOf(criteres);
        return (root, query, cb) -> {
            Contexte<T> ctx = new Contexte<>(root, query, cb);
            return cb.and(liste.stream().map(c -> c.predicat(ctx)).filter(Objects::nonNull).toArray(Predicate[]::new));
        };
    }

    public static boolean renseigne(String s) {
        return s != null && !s.isBlank();
    }

    /** Racine et jointures d'une exécution de la requête (une jointure par association, réutilisée). */
    private static final class Contexte<T> {
        final Root<T> root;
        final CriteriaQuery<?> query;
        final CriteriaBuilder cb;
        private final Map<String, From<?, ?>> jointures = new HashMap<>();

        Contexte(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
            this.root = root;
            this.query = query;
            this.cb = cb;
        }

        <Y> Path<Y> chemin(String chemin) {
            String[] parties = chemin.split("\\.");
            From<?, ?> from = root;
            StringBuilder cle = new StringBuilder();
            for (int i = 0; i < parties.length - 1; i++) {
                cle.append(i > 0 ? "." : "").append(parties[i]);
                From<?, ?> parent = from;
                String attribut = parties[i];
                from = jointures.computeIfAbsent(cle.toString(), k -> parent.join(attribut, JoinType.LEFT));
            }
            return from.get(parties[parties.length - 1]);
        }
    }
}
