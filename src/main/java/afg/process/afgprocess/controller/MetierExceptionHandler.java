package afg.process.afgprocess.controller;

import afg.process.afgprocess.service.exception.MetierException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * Erreur fonctionnelle non traitée par le contrôleur : retour à l'accueil
 * avec le message dans la fenêtre d'erreur (flash « ko », enregistré par la redirection).
 */
@ControllerAdvice
public class MetierExceptionHandler {

    @ExceptionHandler(MetierException.class)
    public String metier(MetierException ex, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("ko", HtmlUtils.htmlEscape(ex.getMessage()));
        return "redirect:/";
    }
}
