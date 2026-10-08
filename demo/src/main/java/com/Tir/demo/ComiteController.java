package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Tout le monde voit le comité, seul l'admin le modifie.
@Controller
@RequestMapping("/comite")
public class ComiteController {

    private final ClubDonnees donnees;
    private final Acces acces;
    private final Club club;

    public ComiteController(ClubDonnees donnees, Acces acces, Club club) {
        this.donnees = donnees;
        this.acces = acces;
        this.club = club;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("membres", donnees.comite);
        return "comite";
    }

    @GetMapping("/ajouter")
    public String ajouterForm() {
        acces.verifierAdmin();
        return "comite-form";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String prenom, @RequestParam String nom, @RequestParam String fonction) {
        acces.verifierAdmin();
        donnees.comite.add(new MembreComite(donnees.nouvelId(), prenom, nom, fonction, club.getId()));
        return "redirect:/comite";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable int id, Model model) {
        acces.verifierAdmin();
        model.addAttribute("membre", ClubDonnees.trouver(donnees.comite, id));
        return "comite-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable int id, @RequestParam String prenom,
                           @RequestParam String nom, @RequestParam String fonction) {
        acces.verifierAdmin();
        ClubDonnees.trouver(donnees.comite, id);
        ClubDonnees.remplacer(donnees.comite, new MembreComite(id, prenom, nom, fonction, club.getId()));
        return "redirect:/comite";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        acces.verifierAdmin();
        ClubDonnees.supprimer(donnees.comite, id);
        return "redirect:/comite";
    }
}
