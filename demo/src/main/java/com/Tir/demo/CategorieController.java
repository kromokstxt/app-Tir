package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Tout le monde voit les catégories de tir (toutes à 300 m), seul l'admin les modifie.
@Controller
@RequestMapping("/categories")
public class CategorieController {

    private final ClubDonnees donnees;
    private final Acces acces;

    public CategorieController(ClubDonnees donnees, Acces acces) {
        this.donnees = donnees;
        this.acces = acces;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("categories", donnees.categories);
        return "categories";
    }

    @GetMapping("/ajouter")
    public String ajouterForm() {
        acces.verifierAdmin();
        return "categorie-form";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String nom) {
        acces.verifierAdmin();
        donnees.categories.add(new CategorieTir(donnees.nouvelId(), nom, CategorieTir.DISTANCE));
        return "redirect:/categories";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable int id, Model model) {
        acces.verifierAdmin();
        model.addAttribute("categorie", ClubDonnees.trouver(donnees.categories, id));
        return "categorie-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable int id, @RequestParam String nom) {
        acces.verifierAdmin();
        ClubDonnees.trouver(donnees.categories, id);
        ClubDonnees.remplacer(donnees.categories, new CategorieTir(id, nom, CategorieTir.DISTANCE));
        return "redirect:/categories";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        acces.verifierAdmin();
        ClubDonnees.supprimer(donnees.categories, id);
        return "redirect:/categories";
    }
}
