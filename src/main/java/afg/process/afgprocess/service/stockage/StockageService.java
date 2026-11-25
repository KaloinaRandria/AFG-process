package afg.process.afgprocess.service.stockage;

import afg.process.afgprocess.service.exception.MetierException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Arrays;

/**
 * Stockage sur le disque du serveur des PDF des procédures :
 * storage.location/procedures/{code de la direction}/{nom d'origine du fichier}.
 * La base ne garde que le chemin relatif (ex. « DSI/Ouverture de compte.pdf »).
 */
@Slf4j
@Service
public class StockageService {

    private static final byte[] SIGNATURE_PDF = {'%', 'P', 'D', 'F', '-'};
    private static final int LONGUEUR_MAX_NOM = 150;

    private final Path dossier;

    public StockageService(@Value("${storage.location}") String location) throws IOException {
        this.dossier = Paths.get(location).toAbsolutePath().normalize().resolve("procedures");
        Files.createDirectories(dossier);
        log.info("Fichiers des procédures stockés dans {}", dossier);
    }

    /** Chemin relatif « {dossier de la direction}/{nom du fichier} », noms nettoyés pour le système de fichiers. */
    public String cheminRelatif(String codeDirection, String nomFichier) {
        String base = nomFichier == null ? "" : nomFichier.replace('\\', '/');
        String nom = nettoyer(base.substring(base.lastIndexOf('/') + 1));   // sans le chemin du poste client
        if (!nom.toLowerCase().endsWith(".pdf")) {
            nom = nom + ".pdf";
        }
        if (nom.equalsIgnoreCase(".pdf")) {
            nom = "procedure.pdf";
        }
        return dossierDirection(codeDirection) + "/" + nom;
    }

    /** Nom du dossier d'une direction (son code nettoyé). */
    public String dossierDirection(String codeDirection) {
        String d = nettoyer(codeDirection);
        return d.isEmpty() ? "SANS_DIRECTION" : d;
    }

    /** Vérifie que le fichier reçu est un PDF puis l'écrit au chemin relatif donné (remplace un fichier existant). */
    public void enregistrerPdf(MultipartFile fichier, String cheminRelatif) {
        if (fichier == null || fichier.isEmpty()) {
            throw new MetierException("Aucun fichier reçu.");
        }
        String nomOrigine = fichier.getOriginalFilename() == null ? "" : fichier.getOriginalFilename().toLowerCase();
        if (!nomOrigine.endsWith(".pdf")) {
            throw new MetierException("Le fichier doit être un PDF (.pdf).");
        }
        try {
            try (InputStream in = fichier.getInputStream()) {
                if (!Arrays.equals(in.readNBytes(SIGNATURE_PDF.length), SIGNATURE_PDF)) {
                    throw new MetierException("Le fichier n'est pas un PDF valide.");
                }
            }
            Path cible = resoudre(cheminRelatif);
            Files.createDirectories(cible.getParent());
            try (InputStream in = fichier.getInputStream()) {
                Files.copy(in, cible, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("PDF enregistré : {}", cible);
        } catch (IOException e) {
            throw new MetierException("Impossible d'enregistrer le fichier : " + e.getMessage());
        }
    }

    public boolean existe(String cheminRelatif) {
        return cheminRelatif != null && !cheminRelatif.isBlank() && Files.exists(resoudre(cheminRelatif));
    }

    /** Déplace / renomme un fichier (ex. procédure changée de direction). */
    public void deplacer(String source, String cible) {
        if (source.equals(cible)) return;
        try {
            Path destination = resoudre(cible);
            Files.createDirectories(destination.getParent());
            Files.move(resoudre(source), destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("PDF déplacé : {} → {}", source, cible);
            supprimerDossierSiVide(resoudre(source).getParent());
        } catch (IOException e) {
            throw new MetierException("Impossible de déplacer le fichier " + source + " : " + e.getMessage());
        }
    }

    /** Renomme le dossier d'une direction (changement de code). Sans effet si l'ancien dossier n'existe pas. */
    public void renommerDossier(String ancienCode, String nouveauCode) {
        Path ancien = resoudre(dossierDirection(ancienCode));
        Path nouveau = resoudre(dossierDirection(nouveauCode));
        if (ancien.equals(nouveau) || !Files.isDirectory(ancien)) return;
        if (Files.exists(nouveau)) {
            throw new MetierException("Le dossier " + nouveau.getFileName() + " existe déjà sur le serveur.");
        }
        try {
            Files.move(ancien, nouveau);
            log.info("Dossier de direction renommé : {} → {}", ancien.getFileName(), nouveau.getFileName());
        } catch (IOException e) {
            throw new MetierException("Impossible de renommer le dossier " + ancien.getFileName() + " : " + e.getMessage());
        }
    }

    public Resource charger(String cheminRelatif) {
        Path chemin = resoudre(cheminRelatif);
        if (!Files.isReadable(chemin)) {
            throw new MetierException("Fichier introuvable sur le serveur.");
        }
        return new PathResource(chemin);
    }

    public void supprimer(String cheminRelatif) {
        if (cheminRelatif == null || cheminRelatif.isBlank()) return;
        try {
            Path chemin = resoudre(cheminRelatif);
            Files.deleteIfExists(chemin);
            supprimerDossierSiVide(chemin.getParent());
        } catch (IOException e) {
            log.warn("Suppression impossible de {} : {}", cheminRelatif, e.getMessage());
        }
    }

    // ── Outils ───────────────────────────────────────────────────────────────

    private void supprimerDossierSiVide(Path d) throws IOException {
        if (d != null && !d.equals(dossier) && Files.isDirectory(d)) {
            try (var contenu = Files.list(d)) {
                if (contenu.findAny().isEmpty()) Files.delete(d);
            }
        }
    }

    /** Résout un chemin relatif en refusant toute sortie du dossier de stockage. */
    private Path resoudre(String cheminRelatif) {
        Path chemin = dossier.resolve(cheminRelatif).normalize();
        if (!chemin.startsWith(dossier) || chemin.equals(dossier)) {
            throw new MetierException("Chemin de fichier invalide.");
        }
        return chemin;
    }

    /** Nom utilisable sous Windows comme sous Linux : caractères interdits remplacés, accents conservés. */
    static String nettoyer(String nom) {
        String n = Normalizer.normalize(nom == null ? "" : nom, Normalizer.Form.NFC)
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .replaceAll("\\s+", " ")
                .trim()
                .replaceAll("^[.\\s]+|[.\\s]+$", "");
        if (n.length() > LONGUEUR_MAX_NOM) {
            int point = n.lastIndexOf('.');
            String ext = point > 0 && n.length() - point <= 5 ? n.substring(point) : "";
            n = n.substring(0, LONGUEUR_MAX_NOM - ext.length()) + ext;
        }
        return n;
    }
}
