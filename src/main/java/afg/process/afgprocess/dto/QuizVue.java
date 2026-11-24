package afg.process.afgprocess.dto;

import java.util.List;

/** Quiz affiché au collaborateur : aucune information sur les bonnes réponses. */
public record QuizVue(int idEvaluation, String procedureTitre, String procedureReference,
                      String direction, int seuil, List<QuestionQuiz> questions) {

    public record QuestionQuiz(int idReponse, int numero, String enonce, List<ChoixQuiz> choix) {
    }

    public record ChoixQuiz(int id, String libelle) {
    }
}
