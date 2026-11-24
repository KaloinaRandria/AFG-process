package afg.process.afgprocess.model.evaluation;

import lombok.Getter;

/** Décision prise à partir de la note du quiz. */
@Getter
public enum ResultatEvaluation {
    ACQUIS("Acquis"),
    FORMATION_REQUISE("Formation requise");

    private final String libelle;

    ResultatEvaluation(String libelle) {
        this.libelle = libelle;
    }
}
