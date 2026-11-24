package afg.process.afgprocess.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Attributs communs à toutes les pages : URI courante (sans le contexte) pour surligner le menu. */
@ControllerAdvice
public class GlobalModelAttribute {

    @ModelAttribute
    public void addCurrentUri(HttpServletRequest request, Model model) {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        model.addAttribute("currentUri", uri.isEmpty() ? "/" : uri);
    }
}
