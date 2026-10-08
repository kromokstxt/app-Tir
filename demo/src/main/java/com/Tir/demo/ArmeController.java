package com.Tir.demo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/armes")
public class ArmeController {

    private final ClubDonnees donnees;
    private final Acces acces;

    public ArmeController(ClubDonnees donnees, Acces acces) {
        this.donnees = donnees;
        this.acces = acces;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("armes", donnees.armes.stream().filter(a -> acces.peutVoir(a.getTireurId())).toList());
        return "armes";
    }

    @GetMapping("/ajouter")
    public String ajouterForm(Model model) {
        model.addAttribute("categories", Arme.CATEGORIES);
        return "arme-form";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String modele, @RequestParam String categorie,
                          @RequestParam(required = false) Integer tireurId) {
        verifierCategorie(categorie);
        donnees.armes.add(new Arme(donnees.nouvelId(), modele, categorie, acces.proprietaire(tireurId)));
        return "redirect:/armes";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable int id, Model model) {
        Arme arme = ClubDonnees.trouver(donnees.armes, id);
        acces.verifierProprietaire(arme.getTireurId());
        model.addAttribute("arme", arme);
        model.addAttribute("categories", Arme.CATEGORIES);
        return "arme-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable int id, @RequestParam String modele, @RequestParam String categorie,
                           @RequestParam(required = false) Integer tireurId) {
        acces.verifierProprietaire(ClubDonnees.trouver(donnees.armes, id).getTireurId());
        verifierCategorie(categorie);
        ClubDonnees.remplacer(donnees.armes, new Arme(id, modele, categorie, acces.proprietaire(tireurId)));
        return "redirect:/armes";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        acces.verifierProprietaire(ClubDonnees.trouver(donnees.armes, id).getTireurId());
        ClubDonnees.supprimer(donnees.armes, id);
        return "redirect:/armes";
    }

    private void verifierCategorie(String categorie) {
        if (!Arme.CATEGORIES.contains(categorie)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie d'arme inconnue");
        }
    }
}
