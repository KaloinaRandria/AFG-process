-- Initialisation : rôle ADMIN et premier compte autorisé à se connecter.
-- À exécuter une fois sur la base de l'application (PROCESS_UAT, PROCESS_PREPROD…), après le premier démarrage
-- de l'application (Hibernate crée les tables role, utilisateur, utilisateur_role).

INSERT INTO role (role)
VALUES ('ADMIN')
ON CONFLICT (role) DO NOTHING;

INSERT INTO utilisateur (nom, prenom, mail, actif)
VALUES ('RANDRIAMBOLOLONA', 'Kaloina', 'kaloina.randriambololona@afgbank.mg', true)
ON CONFLICT (mail) DO NOTHING;

INSERT INTO utilisateur_role (id_utilisateur, id_role)
SELECT u.id_utilisateur, r.id_role
FROM utilisateur u, role r
WHERE u.mail = 'kaloina.randriambololona@afgbank.mg'
  AND r.role = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM utilisateur_role ur
                  WHERE ur.id_utilisateur = u.id_utilisateur AND ur.id_role = r.id_role);
