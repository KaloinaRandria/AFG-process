package afg.process.afgprocess.dto;

import afg.process.afgprocess.model.procedure.Procedure;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProcedureForm {

    private Integer id;

    @NotBlank(message = "La référence est obligatoire")
    @Size(max = 50)
    private String reference;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 255)
    private String titre;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "La direction est obligatoire")
    private Integer idDirection;

    @Min(value = 1, message = "Le seuil doit être compris entre 1 et le nombre de questions")
    @Max(value = 100, message = "Seuil trop élevé")
    private int seuilReussite = 7;

    private boolean actif = true;

    /** Nom du fichier actuel (affichage en modification). */
    private String fichierNom;

    public static ProcedureForm depuis(Procedure p) {
        ProcedureForm f = new ProcedureForm();
        f.id = p.getId();
        f.reference = p.getReference();
        f.titre = p.getTitre();
        f.description = p.getDescription();
        f.idDirection = p.getDirection().getId();
        f.seuilReussite = p.getSeuilReussite();
        f.actif = p.isActif();
        f.fichierNom = p.getFichierNom();
        return f;
    }
}
