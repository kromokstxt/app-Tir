package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Calendrier du club : tout le monde le voit, seul l'admin le modifie.
@Controller
@RequestMapping("/calendrier")
public class CalendrierController {

    private final ClubDonnees donnees;
    private final Acces acces;

    public CalendrierController(ClubDonnees donnees, Acces acces) {
        this.donnees = donnees;
        this.acces = acces;
    }

    // Une case du calendrier.
    public record Jour(LocalDate date, boolean duMois, boolean aujourdhui, boolean choisi, boolean evenement) {}

    // Le calendrier du mois, comme sur un téléphone : on touche un jour pour voir ce qui est prévu.
    @GetMapping
    public String liste(@RequestParam(required = false) String mois, @RequestParam(required = false) String jour,
                        Model model) {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate choisi = lireDate(jour);
        YearMonth leMois = lireMois(mois);
        if (leMois == null) {
            leMois = YearMonth.from(choisi != null ? choisi : aujourdhui);
        }
        if (choisi == null && leMois.equals(YearMonth.from(aujourdhui))) {
            choisi = aujourdhui;
        }

        Set<String> joursAvecEvenement = donnees.calendrier.stream().map(Evenement::getDate).collect(Collectors.toSet());
        List<List<Jour>> semaines = new ArrayList<>();
        LocalDate jourCase = leMois.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate dernier = leMois.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        while (!jourCase.isAfter(dernier)) {
            List<Jour> semaine = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                semaine.add(new Jour(jourCase, YearMonth.from(jourCase).equals(leMois), jourCase.equals(aujourdhui),
                        jourCase.equals(choisi), joursAvecEvenement.contains(jourCase.toString())));
                jourCase = jourCase.plusDays(1);
            }
            semaines.add(semaine);
        }

        model.addAttribute("titreMois", majuscule(leMois.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH))));
        model.addAttribute("mois", leMois.toString());
        model.addAttribute("moisPrecedent", leMois.minusMonths(1).toString());
        model.addAttribute("moisSuivant", leMois.plusMonths(1).toString());
        model.addAttribute("semaines", semaines);
        if (choisi != null) {
            String date = choisi.toString();
            model.addAttribute("jourChoisi", date);
            model.addAttribute("titreJour", majuscule(choisi.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))));
            model.addAttribute("evenementsDuJour", trier(donnees.calendrier.stream().filter(e -> e.getDate().equals(date))));
        }
        // Les prochaines dates, pour ne rien manquer.
        model.addAttribute("prochains", trier(donnees.calendrier.stream()
                .filter(e -> e.getDate().compareTo(aujourdhui.toString()) >= 0)).stream().limit(5).toList());
        return "calendrier";
    }

    private static List<Evenement> trier(Stream<Evenement> evenements) {
        return evenements.sorted(Comparator.comparing(Evenement::getDate).thenComparing(Evenement::getHeure)).toList();
    }

    private static LocalDate lireDate(String texte) {
        try {
            return texte == null ? null : LocalDate.parse(texte);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static YearMonth lireMois(String texte) {
        try {
            return texte == null ? null : YearMonth.parse(texte);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String majuscule(String texte) {
        return texte.substring(0, 1).toUpperCase(Locale.FRENCH) + texte.substring(1);
    }

    @GetMapping("/ajouter")
    public String ajouterForm(@RequestParam(required = false) String date, Model model) {
        acces.verifierAdmin();
        LocalDate jour = lireDate(date);
        model.addAttribute("dateChoisie", jour == null ? null : jour.toString());
        return "evenement-form";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String date, @RequestParam(defaultValue = "") String heure,
                          @RequestParam String titre, @RequestParam(defaultValue = "") String description) {
        acces.verifierAdmin();
        donnees.calendrier.add(new Evenement(donnees.nouvelId(), date, heure, titre, description));
        return "redirect:/calendrier";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable int id, Model model) {
        acces.verifierAdmin();
        model.addAttribute("evenement", ClubDonnees.trouver(donnees.calendrier, id));
        return "evenement-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable int id, @RequestParam String date, @RequestParam(defaultValue = "") String heure,
                           @RequestParam String titre, @RequestParam(defaultValue = "") String description) {
        acces.verifierAdmin();
        ClubDonnees.trouver(donnees.calendrier, id);
        ClubDonnees.remplacer(donnees.calendrier, new Evenement(id, date, heure, titre, description));
        return "redirect:/calendrier";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        acces.verifierAdmin();
        ClubDonnees.supprimer(donnees.calendrier, id);
        return "redirect:/calendrier";
    }
}
