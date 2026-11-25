package afg.process.afgprocess.controller.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

/**
 * Règle de pagination et de tri d'une liste, déclarée une fois par liste dans le contrôleur :
 * <pre>
 * private static final ListeTri TRI = ListeTri.par(Sort.by("nom"), "nom", "mail", "direction.code");
 * ... service.rechercher(filtre, TRI.appliquer(pageable))
 * </pre>
 * Seules les colonnes déclarées peuvent être triées (un tri inconnu provoquerait une erreur SQL) ;
 * le tri par défaut complète toujours le tri demandé ; la taille de page est bornée.
 */
public record ListeTri(Sort parDefaut, Set<String> colonnes) {

    public static final int TAILLE_MAX = 100;

    public static ListeTri par(Sort parDefaut, String... colonnes) {
        return new ListeTri(parDefaut, Set.of(colonnes));
    }

    public Pageable appliquer(Pageable demande) {
        List<Sort.Order> ordres = demande.getSort().stream()
                .filter(o -> colonnes.contains(o.getProperty()))
                .map(Sort.Order::nullsLast)
                .toList();
        Sort tri = ordres.isEmpty() ? parDefaut : Sort.by(ordres).and(parDefaut);
        int taille = Math.min(Math.max(demande.getPageSize(), 1), TAILLE_MAX);
        return PageRequest.of(demande.getPageNumber(), taille, tri);
    }

    /** Tri à utiliser sans pagination (export), avec les mêmes règles. */
    public Sort appliquer(Sort demande) {
        return appliquer(PageRequest.of(0, 1, demande)).getSort();
    }
}
