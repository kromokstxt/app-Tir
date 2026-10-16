-- Les tables de la base SQLite (fichier club.db). Créées au premier démarrage, jamais effacées.
-- Ouvrable avec « DB Browser for SQLite » pour regarder les données.

CREATE TABLE IF NOT EXISTS tireur (
    id            INTEGER PRIMARY KEY,
    prenom        TEXT    NOT NULL,
    nom           TEXT    NOT NULL,
    club_id       INTEGER NOT NULL,
    identifiant   TEXT    NOT NULL UNIQUE,   -- interne, jamais affiché (ex. jean.dupont)
    mot_de_passe  TEXT    NOT NULL,          -- brouillé (BCrypt), illisible
    admin         INTEGER NOT NULL           -- 1 = admin
);

CREATE TABLE IF NOT EXISTS arme (
    id         INTEGER PRIMARY KEY,
    categorie  TEXT    NOT NULL,   -- Fas 90, Fas 57, Mousqueton, Fusil de sport
    version    TEXT    NOT NULL,   -- 02 ou 03 pour un Fas 57, sinon vide
    tireur_id  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS licence (
    id         INTEGER PRIMARY KEY,
    numero     TEXT    NOT NULL,
    tireur_id  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS resultat (
    id              INTEGER PRIMARY KEY,
    tireur_id       INTEGER NOT NULL,
    saison_id       INTEGER NOT NULL,   -- 0 = hors saison
    date            TEXT    NOT NULL,   -- 2026-10-16
    arme            TEXT    NOT NULL,   -- ex. Fas 57/03
    externe         INTEGER NOT NULL,   -- 1 = tir externe
    coups_profonds  INTEGER NOT NULL,   -- 1 = avec coups profonds
    coups           TEXT    NOT NULL    -- ex. « 9:87>HD 10:96 M »
);

CREATE TABLE IF NOT EXISTS saison (
    id          INTEGER PRIMARY KEY,
    annee       TEXT NOT NULL,
    date_debut  TEXT NOT NULL,
    date_fin    TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS categorie (
    id        INTEGER PRIMARY KEY,
    nom       TEXT    NOT NULL,
    distance  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS classement (
    id         INTEGER PRIMARY KEY,
    position   INTEGER NOT NULL,
    points     INTEGER NOT NULL,
    saison_id  INTEGER NOT NULL,
    tireur_id  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS annonce (
    id       INTEGER PRIMARY KEY,
    titre    TEXT NOT NULL,
    message  TEXT NOT NULL,
    date     TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS evenement (
    id           INTEGER PRIMARY KEY,
    date         TEXT NOT NULL,
    heure        TEXT NOT NULL,
    titre        TEXT NOT NULL,
    description  TEXT NOT NULL
);
