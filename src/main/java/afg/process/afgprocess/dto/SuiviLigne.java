package afg.process.afgprocess.dto;

import afg.process.afgprocess.model.evaluation.Evaluation;

/** Dernier résultat d'un collaborateur sur une procédure, avec le nombre de tentatives. */
public record SuiviLigne(Evaluation derniere, long tentatives) {
}
