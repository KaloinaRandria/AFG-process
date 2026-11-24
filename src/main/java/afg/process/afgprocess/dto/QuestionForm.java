package afg.process.afgprocess.dto;

import afg.process.afgprocess.model.procedure.Choix;
import afg.process.afgprocess.model.procedure.Question;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Saisie d'une question : énoncé, jusqu'à 6 propositions, index de la bonne réponse. */
@Getter
@Setter
@NoArgsConstructor
public class QuestionForm {

    public static final int NB_CHOIX_MAX = 6;

    private Integer id;
    private String enonce;
    private List<String> choix = new ArrayList<>(List.of("", "", "", ""));
    private Integer indexCorrect;
    private boolean actif = true;

    public static QuestionForm depuis(Question q) {
        QuestionForm f = new QuestionForm();
        f.id = q.getId();
        f.enonce = q.getEnonce();
        f.actif = q.isActif();
        f.choix = new ArrayList<>();
        for (int i = 0; i < q.getChoix().size(); i++) {
            Choix c = q.getChoix().get(i);
            f.choix.add(c.getLibelle());
            if (c.isCorrect()) f.indexCorrect = i;
        }
        while (f.choix.size() < 4) f.choix.add("");
        return f;
    }
}
