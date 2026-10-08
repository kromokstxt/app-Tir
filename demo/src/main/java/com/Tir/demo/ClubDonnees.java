package com.Tir.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

// Toutes les données du club, gardées en mémoire (perdues au redémarrage).
@Component
public class ClubDonnees {

    public final List<Shooter> tireurs = new CopyOnWriteArrayList<>();
    public final List<Arme> armes = new CopyOnWriteArrayList<>();
    public final List<Licence> licences = new CopyOnWriteArrayList<>();
    public final List<Seance> seances = new CopyOnWriteArrayList<>();
    public final List<Resultat> resultats = new CopyOnWriteArrayList<>();
    public final List<Saison> saisons = new CopyOnWriteArrayList<>();
    public final List<CategorieTir> categories = new CopyOnWriteArrayList<>();
    public final List<Classement> classements = new CopyOnWriteArrayList<>();
    public final List<Annonce> annonces = new CopyOnWriteArrayList<>();
    public final List<Evenement> calendrier = new CopyOnWriteArrayList<>();

    private final AtomicInteger prochainId = new AtomicInteger(1);

    public ClubDonnees(Club club, PasswordEncoder encoder,
                       @Value("${app.admin.motdepasse}") String motDePasseAdmin) {
        tireurs.add(new Shooter(nouvelId(), "Admin", "Club", club.getId(), "admin", encoder.encode(motDePasseAdmin), true));
        tireurs.add(new Shooter(nouvelId(), "xxxx", "yyy", club.getId(), "tireur1", encoder.encode("tireur1"), false));
        tireurs.add(new Shooter(nouvelId(), "xxxx", "yyy", club.getId(), "tireur2", encoder.encode("tireur2"), false));
        for (String categorie : Arme.CATEGORIES) {
            categories.add(new CategorieTir(nouvelId(), categorie, CategorieTir.DISTANCE));
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

    public Shooter tireurParUsername(String username) {
        return tireurs.stream().filter(t -> t.getUsername().equals(username)).findFirst().orElse(null);
    }

    // Supprime un tireur et tout ce qui lui appartient.
    public void supprimerTireur(int id) {
        supprimer(tireurs, id);
        armes.removeIf(a -> a.getTireurId() == id);
        licences.removeIf(l -> l.getTireurId() == id);
        classements.removeIf(c -> c.getTireurId() == id);
        seances.stream().filter(s -> s.getTireurId() == id).forEach(s -> supprimerSeance(s.getId()));
    }

    public void supprimerSeance(int id) {
        supprimer(seances, id);
        resultats.removeIf(r -> r.getSeanceId() == id);
    }

    // La saison qui contient cette date (les dates sont au format 2026-10-08), 0 s'il n'y en a pas.
    public int saisonPour(String date) {
        return saisons.stream()
                .filter(s -> s.getDateDebut().compareTo(date) <= 0 && date.compareTo(s.getDateFin()) <= 0)
                .mapToInt(Saison::getId).findFirst().orElse(0);
    }

    // Le tireur à qui appartient une séance (-1 si elle n'existe plus).
    public int proprietaireSeance(int seanceId) {
        return seances.stream().filter(s -> s.getId() == seanceId)
                .mapToInt(Seance::getTireurId).findFirst().orElse(-1);
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

    public String nomSeance(int id) {
        return seances.stream().filter(s -> s.getId() == id)
                .map(s -> s.getDate() + " – " + s.getType() + " (" + nomTireur(s.getTireurId()) + ")")
                .findFirst().orElse("?");
    }
}
