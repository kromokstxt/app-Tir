package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// Un résultat appartient au tireur de sa séance.
@Controller
@RequestMapping("/resultats")
public class ResultatController {

    private final ClubDonnees donnees;
    private final Acces acces;

    public ResultatController(ClubDonnees donnees, Acces acces) {
        this.donnees = donnees;
        this.acces = acces;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("resultats", donnees.resultats.stream()
                .filter(r -> acces.peutVoir(donnees.proprietaireSeance(r.getSeanceId()))).toList());
        return "resultats";
    }

    @GetMapping("/ajouter")
    public String ajouterForm(Model model) {
        model.addAttribute("mesSeances", mesSeances());
        return "resultat-form";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam int score, @RequestParam String date,
                          @RequestParam int seanceId, @RequestParam int categorieId) {
        verifierSeanceEtCategorie(seanceId, categorieId);
        donnees.resultats.add(new Resultat(donnees.nouvelId(), score, date, seanceId, categorieId));
        return "redirect:/resultats";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable int id, Model model) {
        Resultat resultat = ClubDonnees.trouver(donnees.resultats, id);
        acces.verifierProprietaire(donnees.proprietaireSeance(resultat.getSeanceId()));
        model.addAttribute("resultat", resultat);
        model.addAttribute("mesSeances", mesSeances());
        return "resultat-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable int id, @RequestParam int score, @RequestParam String date,
                           @RequestParam int seanceId, @RequestParam int categorieId) {
        Resultat ancien = ClubDonnees.trouver(donnees.resultats, id);
        acces.verifierProprietaire(donnees.proprietaireSeance(ancien.getSeanceId()));
        verifierSeanceEtCategorie(seanceId, categorieId);
        ClubDonnees.remplacer(donnees.resultats, new Resultat(id, score, date, seanceId, categorieId));
        return "redirect:/resultats";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        Resultat resultat = ClubDonnees.trouver(donnees.resultats, id);
        acces.verifierProprietaire(donnees.proprietaireSeance(resultat.getSeanceId()));
        ClubDonnees.supprimer(donnees.resultats, id);
        return "redirect:/resultats";
    }

    private List<Seance> mesSeances() {
        return donnees.seances.stream().filter(s -> acces.peutVoir(s.getTireurId())).toList();
    }

    private void verifierSeanceEtCategorie(int seanceId, int categorieId) {
        acces.verifierProprietaire(ClubDonnees.trouver(donnees.seances, seanceId).getTireurId());
        ClubDonnees.trouver(donnees.categories, categorieId);
    }
}
