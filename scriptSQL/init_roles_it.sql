-- Gestion des utilisateurs par l'IT (circuit maker-checker).
--
--   IT        : consulte les utilisateurs et les demandes
--   IT_MAKER  : saisit les demandes de création / modification d'utilisateurs
--   CHECKER   : valide ou rejette les demandes saisies par une AUTRE personne
--
-- Les rôles sont créés automatiquement au démarrage de l'application (RolesInitializer) ;
-- les INSERT ci-dessous ne servent que si le script est passé avant.
--
-- Amorçage : comme personne ne peut valider sa propre demande, il faut au départ
-- un IT_MAKER et un CHECKER qui soient deux personnes différentes. Ces deux premiers
-- comptes sont créés ici ; tous les suivants passent par l'application.
-- Remplacer les adresses ci-dessous avant exécution.

INSERT INTO role (role) VALUES ('IT')       ON CONFLICT (role) DO NOTHING;
INSERT INTO role (role) VALUES ('IT_MAKER') ON CONFLICT (role) DO NOTHING;
INSERT INTO role (role) VALUES ('CHECKER')  ON CONFLICT (role) DO NOTHING;

-- ── Premier IT_MAKER ─────────────────────────────────────────────────────────
INSERT INTO utilisateur (nom, prenom, mail, actif)
VALUES ('NOM_MAKER', 'Prenom', 'maker.it@afgbank.mg', true)
ON CONFLICT (mail) DO NOTHING;

INSERT INTO utilisateur_role (id_utilisateur, id_role)
SELECT u.id_utilisateur, r.id_role
FROM utilisateur u, role r
WHERE u.mail = 'maker.it@afgbank.mg'
  AND r.role IN ('IT', 'IT_MAKER')
  AND NOT EXISTS (SELECT 1 FROM utilisateur_role ur
                  WHERE ur.id_utilisateur = u.id_utilisateur AND ur.id_role = r.id_role);

-- ── Premier CHECKER (une autre personne) ─────────────────────────────────────
INSERT INTO utilisateur (nom, prenom, mail, actif)
VALUES ('NOM_CHECKER', 'Prenom', 'checker.it@afgbank.mg', true)
ON CONFLICT (mail) DO NOTHING;

INSERT INTO utilisateur_role (id_utilisateur, id_role)
SELECT u.id_utilisateur, r.id_role
FROM utilisateur u, role r
WHERE u.mail = 'checker.it@afgbank.mg'
  AND r.role IN ('IT', 'CHECKER')
  AND NOT EXISTS (SELECT 1 FROM utilisateur_role ur
                  WHERE ur.id_utilisateur = u.id_utilisateur AND ur.id_role = r.id_role);
