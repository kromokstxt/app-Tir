package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;

// Annonces du comité : tout le monde les lit, seul l'admin les écrit.
@Controller
@RequestMapping("/annonces")
public class AnnonceController {

    private final ClubDonnees donnees;
    private final Acces acces;

    public AnnonceController(ClubDonnees donnees, Acces acces) {
        this.donnees = donnees;
        this.acces = acces;
    }

    @GetMapping
    public String liste(Model model) {
        // Les plus récentes en premier.
        model.addAttribute("annonces", donnees.annonces.stream()
                .sorted(Comparator.comparing(Annonce::getDate).reversed()).toList());
        return "annonces";
    }

    @GetMapping("/ajouter")
    public String ajouterForm() {
        acces.verifierAdmin();
        return "annonce-form";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String titre, @RequestParam String message, @RequestParam String date) {
        acces.verifierAdmin();
        donnees.annonces.add(new Annonce(donnees.nouvelId(), titre, message, date));
        return "redirect:/annonces";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable int id, Model model) {
        acces.verifierAdmin();
        model.addAttribute("annonce", ClubDonnees.trouver(donnees.annonces, id));
        return "annonce-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable int id, @RequestParam String titre,
                           @RequestParam String message, @RequestParam String date) {
        acces.verifierAdmin();
        ClubDonnees.trouver(donnees.annonces, id);
        ClubDonnees.remplacer(donnees.annonces, new Annonce(id, titre, message, date));
        return "redirect:/annonces";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        acces.verifierAdmin();
        ClubDonnees.supprimer(donnees.annonces, id);
        return "redirect:/annonces";
    }
}
