package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Tout le monde voit les saisons, seul l'admin les modifie.
@Controller
@RequestMapping("/saisons")
public class SaisonController {

    private final ClubDonnees donnees;
    private final Acces acces;

    public SaisonController(ClubDonnees donnees, Acces acces) {
        this.donnees = donnees;
        this.acces = acces;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("saisons", donnees.saisons);
        return "saisons";
    }

    @GetMapping("/ajouter")
    public String ajouterForm() {
        acces.verifierAdmin();
        return "saison-form";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String annee, @RequestParam String dateDebut, @RequestParam String dateFin) {
        acces.verifierAdmin();
        donnees.saisons.add(new Saison(donnees.nouvelId(), annee, dateDebut, dateFin));
        return "redirect:/saisons";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable int id, Model model) {
        acces.verifierAdmin();
        model.addAttribute("saison", ClubDonnees.trouver(donnees.saisons, id));
        return "saison-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable int id, @RequestParam String annee,
                           @RequestParam String dateDebut, @RequestParam String dateFin) {
        acces.verifierAdmin();
        ClubDonnees.trouver(donnees.saisons, id);
        ClubDonnees.remplacer(donnees.saisons, new Saison(id, annee, dateDebut, dateFin));
        return "redirect:/saisons";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        acces.verifierAdmin();
        ClubDonnees.supprimer(donnees.saisons, id);
        return "redirect:/saisons";
    }
}
