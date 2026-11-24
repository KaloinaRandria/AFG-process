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
import java.util.Arrays;
import java.util.UUID;

/** Stockage sur disque des fichiers PDF des procédures (répertoire storage.location/procedures). */
@Slf4j
@Service
public class StockageService {

    private static final byte[] SIGNATURE_PDF = {'%', 'P', 'D', 'F', '-'};

    private final Path dossier;

    public StockageService(@Value("${storage.location}") String location) throws IOException {
        this.dossier = Paths.get(location).toAbsolutePath().normalize().resolve("procedures");
        Files.createDirectories(dossier);
        log.info("Fichiers des procédures stockés dans {}", dossier);
    }

    /** Enregistre le PDF sous un nom généré et renvoie ce nom. */
    public String enregistrerPdf(MultipartFile fichier) {
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
            String nom = UUID.randomUUID() + ".pdf";
            try (InputStream in = fichier.getInputStream()) {
                Files.copy(in, dossier.resolve(nom), StandardCopyOption.REPLACE_EXISTING);
            }
            return nom;
        } catch (IOException e) {
            throw new MetierException("Impossible d'enregistrer le fichier : " + e.getMessage());
        }
    }

    public Resource charger(String nom) {
        Path chemin = resoudre(nom);
        if (!Files.isReadable(chemin)) {
            throw new MetierException("Fichier introuvable sur le serveur.");
        }
        return new PathResource(chemin);
    }

    public void supprimer(String nom) {
        if (nom == null || nom.isBlank()) return;
        try {
            Files.deleteIfExists(resoudre(nom));
        } catch (IOException e) {
            log.warn("Suppression impossible de {} : {}", nom, e.getMessage());
        }
    }

    private Path resoudre(String nom) {
        Path chemin = dossier.resolve(nom).normalize();
        if (!chemin.getParent().equals(dossier)) {
            throw new MetierException("Nom de fichier invalide.");
        }
        return chemin;
    }
}
