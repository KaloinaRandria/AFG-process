package afg.process.afgprocess.service.exception;

/** Erreur fonctionnelle : son message est affiché tel quel à l'utilisateur. */
public class MetierException extends RuntimeException {
    public MetierException(String message) {
        super(message);
    }
}
