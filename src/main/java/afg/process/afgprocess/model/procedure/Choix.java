package afg.process.afgprocess.model.procedure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Proposition de réponse d'une question. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "choix")
public class Choix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_choix")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_question", nullable = false)
    private Question question;

    @Column(nullable = false, length = 500)
    private String libelle;

    @Column(nullable = false)
    private boolean correct;

    public Choix(String libelle, boolean correct) {
        this.libelle = libelle;
        this.correct = correct;
    }
}
