package afg.process.afgprocess.controller.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Liens des listes paginées, exposés aux pages comme variable « liste » (GlobalModelAttribute) :
 * changement de page, de taille, de tri, en conservant les filtres de l'URL courante.
 * <pre>th:href="${liste.page(2)}"   th:href="${liste.tri('nom')}"</pre>
 */
@Component("liste")
public class ListeHelper {

    /** Lien vers la page n (numérotée à partir de 0). */
    public String page(int n) {
        return url().replaceQueryParam("page", n).build(true).toUriString();
    }

    /** Lien changeant le nombre de lignes par page (retour à la première page). */
    public String taille(int n) {
        return url().replaceQueryParam("size", n).replaceQueryParam("page").build(true).toUriString();
    }

    /** Lien de tri sur une colonne : croissant, puis décroissant au clic suivant. */
    public String tri(String propriete) {
        String sens = (propriete + ",asc").equals(triCourant()) ? "desc" : "asc";
        return url().replaceQueryParam("sort", propriete + "," + sens).replaceQueryParam("page")
                .build(true).toUriString();
    }

    /** Icône Font Awesome de l'état du tri de la colonne. */
    public String triIcone(String propriete) {
        String courant = triCourant();
        if ((propriete + ",asc").equals(courant)) return "fa-sort-up";
        if ((propriete + ",desc").equals(courant)) return "fa-sort-down";
        return "fa-sort text-muted";
    }

    /** Lien remplaçant un paramètre (ex. l'onglet affiché), en revenant à la première page et au tri par défaut. */
    public String avec(String parametre, String valeur) {
        return url().replaceQueryParam(parametre, UriUtils.encodeQueryParam(valeur, StandardCharsets.UTF_8))
                .replaceQueryParam("page").replaceQueryParam("sort").build(true).toUriString();
    }

    /** Lien vers une sous-adresse de la liste avec les mêmes filtres et le même tri (ex. « /export »). */
    public String memesFiltres(String suffixe) {
        HttpServletRequest r = requete();
        return UriComponentsBuilder.fromPath(r.getRequestURI() + suffixe).query(r.getQueryString())
                .replaceQueryParam("page").replaceQueryParam("size").build(true).toUriString();
    }

    /** Lien « Réinitialiser » : la liste sans aucun filtre, en gardant seulement les paramètres donnés. */
    public String reinitialiser(String... aGarder) {
        HttpServletRequest r = requete();
        UriComponentsBuilder b = UriComponentsBuilder.fromPath(r.getRequestURI());
        for (String p : aGarder) {
            String v = r.getParameter(p);
            if (v != null && !v.isEmpty()) b.queryParam(p, UriUtils.encodeQueryParam(v, StandardCharsets.UTF_8));
        }
        return b.build(true).toUriString();
    }

    /** Valeur d'un paramètre de la requête (champs cachés des filtres), ou null. */
    public String parametre(String nom) {
        if (nom == null || nom.isEmpty()) return null;
        String v = requete().getParameter(nom);
        return v == null || v.isEmpty() ? null : v;
    }

    /** Tri courant de la requête (ex. « nom,asc »), ou null. */
    public String triCourant() {
        String s = requete().getParameter("sort");
        if (s == null || s.isBlank()) return null;
        return s.contains(",") ? s : s + ",asc";
    }

    /** Numéros des pages à afficher autour de la page courante (au plus 7). */
    public List<Integer> fenetre(Page<?> page) {
        int total = page.getTotalPages();
        int debut = Math.max(0, Math.min(page.getNumber() - 3, total - 7));
        int fin = Math.min(total - 1, debut + 6);
        List<Integer> pages = new ArrayList<>();
        for (int i = debut; i <= fin; i++) pages.add(i);
        return pages;
    }

    private UriComponentsBuilder url() {
        HttpServletRequest r = requete();
        return UriComponentsBuilder.fromPath(r.getRequestURI()).query(r.getQueryString());
    }

    private static HttpServletRequest requete() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
