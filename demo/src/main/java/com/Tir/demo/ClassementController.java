package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;

// Tout le monde voit le classement, seul l'admin le modifie.
@Controller
@RequestMapping("/classement")
public class ClassementController {

    private final ClubDonnees donnees;
    private final Acces acces;

    public ClassementController(ClubDonnees donnees, Acces acces) {
        this.donnees = donnees;
        this.acces = acces;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("classements", donnees.classements.stream()
                .sorted(Comparator.comparing((Classement c) -> donnees.nomSaison(c.getSaisonId()))
                        .thenComparingInt(Classement::getPosition))
                .toList());
        return "classement";
    }

    @GetMapping("/ajouter")
    public String ajouterForm() {
        acces.verifierAdmin();
        return "classement-form";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam int position, @RequestParam int points,
                          @RequestParam int saisonId, @RequestParam int tireurId) {
        acces.verifierAdmin();
        verifierLiens(saisonId, tireurId);
        donnees.classements.add(new Classement(donnees.nouvelId(), position, points, saisonId, tireurId));
        return "redirect:/classement";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable int id, Model model) {
        acces.verifierAdmin();
        model.addAttribute("classement", ClubDonnees.trouver(donnees.classements, id));
        return "classement-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable int id, @RequestParam int position, @RequestParam int points,
                           @RequestParam int saisonId, @RequestParam int tireurId) {
        acces.verifierAdmin();
        ClubDonnees.trouver(donnees.classements, id);
        verifierLiens(saisonId, tireurId);
        ClubDonnees.remplacer(donnees.classements, new Classement(id, position, points, saisonId, tireurId));
        return "redirect:/classement";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        acces.verifierAdmin();
        ClubDonnees.supprimer(donnees.classements, id);
        return "redirect:/classement";
    }

    private void verifierLiens(int saisonId, int tireurId) {
        ClubDonnees.trouver(donnees.saisons, saisonId);
        ClubDonnees.trouver(donnees.tireurs, tireurId);
    }
}
