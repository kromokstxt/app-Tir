package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;

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

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("evenements", donnees.calendrier.stream()
                .sorted(Comparator.comparing(Evenement::getDate).thenComparing(Evenement::getHeure)).toList());
        return "calendrier";
    }

    @GetMapping("/ajouter")
    public String ajouterForm() {
        acces.verifierAdmin();
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
