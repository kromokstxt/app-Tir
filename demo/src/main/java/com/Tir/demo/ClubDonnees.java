package com.Tir.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

// Toutes les données du club. Gardées en mémoire pour aller vite, et recopiées à chaque
// changement dans la base SQLite (le fichier club.db) : rien n'est perdu au redémarrage.
@Component
public class ClubDonnees {

    public final List<Shooter> tireurs;
    public final List<Arme> armes;
    public final List<Licence> licences;
    public final List<Resultat> resultats;
    public final List<Saison> saisons;
    public final List<CategorieTir> categories;
    public final List<Classement> classements;
    public final List<Annonce> annonces;
    public final List<Evenement> calendrier;

    private final AtomicInteger prochainId;

    // Au premier démarrage (base vide), on crée l'admin, les catégories et, si demandé, deux tireurs d'exemple.
    public ClubDonnees(JdbcTemplate jdbc, Club club, PasswordEncoder encoder,
                       @Value("${app.admin.motdepasse}") String motDePasseAdmin,
                       @Value("${app.exemples:false}") boolean exemples) {
        List<List<?>> toutes = List.of(
                tireurs = new ListePersistante<>(new Table<>(jdbc, "tireur",
                        List.of("prenom", "nom", "club_id", "identifiant", "mot_de_passe", "admin"),
                        (r, i) -> new Shooter(r.getInt("id"), r.getString("prenom"), r.getString("nom"), r.getInt("club_id"),
                                r.getString("identifiant"), r.getString("mot_de_passe"), r.getInt("admin") == 1),
                        t -> List.of(t.getFirstName(), t.getLastName(), t.getClubId(), t.getUsername(), t.getPassword(),
                                Table.nombre(t.isAdmin())))),
                armes = new ListePersistante<>(new Table<>(jdbc, "arme",
                        List.of("categorie", "version", "tireur_id"),
                        (r, i) -> new Arme(r.getInt("id"), r.getString("categorie"), r.getString("version"), r.getInt("tireur_id")),
                        a -> List.of(a.getCategorie(), a.getVersion(), a.getTireurId()))),
                licences = new ListePersistante<>(new Table<>(jdbc, "licence",
                        List.of("numero", "tireur_id"),
                        (r, i) -> new Licence(r.getInt("id"), r.getString("numero"), r.getInt("tireur_id")),
                        l -> List.of(l.getNumero(), l.getTireurId()))),
                resultats = new ListePersistante<>(new Table<>(jdbc, "resultat",
                        List.of("tireur_id", "saison_id", "date", "arme", "externe", "coups_profonds", "coups"),
                        (r, i) -> new Resultat(r.getInt("id"), r.getInt("tireur_id"), r.getInt("saison_id"), r.getString("date"),
                                r.getString("arme"), r.getInt("externe") == 1, r.getInt("coups_profonds") == 1,
                                List.of(r.getString("coups").split(" "))),
                        x -> List.of(x.getTireurId(), x.getSaisonId(), x.getDate(), x.getArme(), Table.nombre(x.isExterne()),
                                Table.nombre(x.isCoupsProfonds()), x.getCoupsTexte()))),
                saisons = new ListePersistante<>(new Table<>(jdbc, "saison",
                        List.of("annee", "date_debut", "date_fin"),
                        (r, i) -> new Saison(r.getInt("id"), r.getString("annee"), r.getString("date_debut"), r.getString("date_fin")),
                        s -> List.of(s.getAnnee(), s.getDateDebut(), s.getDateFin()))),
                categories = new ListePersistante<>(new Table<>(jdbc, "categorie",
                        List.of("nom", "distance"),
                        (r, i) -> new CategorieTir(r.getInt("id"), r.getString("nom"), r.getInt("distance")),
                        c -> List.of(c.getNom(), c.getDistance()))),
                classements = new ListePersistante<>(new Table<>(jdbc, "classement",
                        List.of("position", "points", "saison_id", "tireur_id"),
                        (r, i) -> new Classement(r.getInt("id"), r.getInt("position"), r.getInt("points"),
                                r.getInt("saison_id"), r.getInt("tireur_id")),
                        c -> List.of(c.getPosition(), c.getPoints(), c.getSaisonId(), c.getTireurId()))),
                annonces = new ListePersistante<>(new Table<>(jdbc, "annonce",
                        List.of("titre", "message", "date"),
                        (r, i) -> new Annonce(r.getInt("id"), r.getString("titre"), r.getString("message"), r.getString("date")),
                        a -> List.of(a.getTitre(), a.getMessage(), a.getDate()))),
                calendrier = new ListePersistante<>(new Table<>(jdbc, "evenement",
                        List.of("date", "heure", "titre", "description"),
                        (r, i) -> new Evenement(r.getInt("id"), r.getString("date"), r.getString("heure"),
                                r.getString("titre"), r.getString("description")),
                        e -> List.of(e.getDate(), e.getHeure(), e.getTitre(), e.getDescription()))));

        // Un seul compteur pour tous les numéros : on repart après le plus grand déjà dans la base.
        prochainId = new AtomicInteger(1 + toutes.stream().mapToInt(l -> ((ListePersistante<?>) l).plusGrandId()).max().orElse(0));

        if (tireurs.isEmpty()) {
            tireurs.add(new Shooter(nouvelId(), "Admin", "Club", club.getId(), "admin", encoder.encode(motDePasseAdmin), true));
            if (exemples) {
                tireurs.add(new Shooter(nouvelId(), "Pierre", "Exemple", club.getId(), "tireur1", encoder.encode("tireur1"), false));
                tireurs.add(new Shooter(nouvelId(), "Marie", "Exemple", club.getId(), "tireur2", encoder.encode("tireur2"), false));
            }
        }
        if (categories.isEmpty()) {
            for (String categorie : Arme.CATEGORIES) {
                categories.add(new CategorieTir(nouvelId(), categorie, CategorieTir.DISTANCE));
            }
        }
    }

    public int nouvelId() {
        return prochainId.getAndIncrement();
    }

    public static <T extends Identifiable> T trouver(List<T> liste, int id) {
        return liste.stream()
                .filter(x -> x.getId() == id)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public static <T extends Identifiable> void remplacer(List<T> liste, T nouveau) {
        liste.replaceAll(x -> x.getId() == nouveau.getId() ? nouveau : x);
    }

    public static <T extends Identifiable> void supprimer(List<T> liste, int id) {
        liste.removeIf(x -> x.getId() == id);
    }

    // Pour comparer des noms : minuscules, sans accents, espaces simples (« Jérôme  Dupont » → « jerome dupont »).
    public static String cle(String texte) {
        String sansAccents = Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    // On se connecte avec son nom (« Jean Dupont » ou « Dupont Jean »), ou avec son identifiant (admin).
    public Shooter tireurPourConnexion(String saisie) {
        String c = cle(saisie);
        return tireurs.stream()
                .filter(t -> c.equals(cle(t.getUsername()))
                        || c.equals(cle(t.getFirstName() + " " + t.getLastName()))
                        || c.equals(cle(t.getLastName() + " " + t.getFirstName())))
                .findFirst().orElse(null);
    }

    // Vrai si un autre tireur porte déjà ce prénom et ce nom (on ne pourrait plus savoir qui se connecte).
    public boolean nomPris(String prenom, String nom, int saufId) {
        String c = cle(prenom + " " + nom);
        return tireurs.stream().anyMatch(t -> t.getId() != saufId && c.equals(cle(t.getFirstName() + " " + t.getLastName())));
    }

    // Identifiant interne, jamais affiché : « jean.dupont », ou « jean.dupont2 » s'il existe déjà.
    public String nouvelIdentifiant(String prenom, String nom) {
        String base = cle(prenom + "." + nom).replace(' ', '-');
        String identifiant = base;
        for (int n = 2; tireurParUsername(identifiant) != null; n++) {
            identifiant = base + n;
        }
        return identifiant;
    }

    public Shooter tireurParUsername(String username) {
        return tireurs.stream().filter(t -> t.getUsername().equals(username)).findFirst().orElse(null);
    }

    // Supprime un tireur et tout ce qui lui appartient.
    public void supprimerTireur(int id) {
        supprimer(tireurs, id);
        armes.removeIf(a -> a.getTireurId() == id);
        licences.removeIf(l -> l.getTireurId() == id);
        classements.removeIf(c -> c.getTireurId() == id);
        resultats.removeIf(r -> r.getTireurId() == id);
    }

    // La saison qui contient cette date (les dates sont au format 2026-10-08), 0 s'il n'y en a pas.
    public int saisonPour(String date) {
        return saisons.stream()
                .filter(s -> s.getDateDebut().compareTo(date) <= 0 && date.compareTo(s.getDateFin()) <= 0)
                .mapToInt(Saison::getId).findFirst().orElse(0);
    }

    // Noms affichés dans les pages (« ? » si l'élément a été supprimé).

    public String nomTireur(int id) {
        return tireurs.stream().filter(t -> t.getId() == id)
                .map(t -> t.getFirstName() + " " + t.getLastName()).findFirst().orElse("?");
    }

    public String nomSaison(int id) {
        return saisons.stream().filter(s -> s.getId() == id).map(Saison::getAnnee).findFirst().orElse("—");
    }

    public String nomCategorie(int id) {
        return categories.stream().filter(c -> c.getId() == id).map(CategorieTir::getNom).findFirst().orElse("?");
    }

}
