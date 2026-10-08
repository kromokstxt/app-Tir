package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// La saison d'une séance est trouvée automatiquement d'après sa date.
@Controller
@RequestMapping("/seances")
public class SeanceController {

    private final ClubDonnees donnees;
    private final Acces acces;

    public SeanceController(ClubDonnees donnees, Acces acces) {
        this.donnees = donnees;
        this.acces = acces;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("seances", donnees.seances.stream().filter(s -> acces.peutVoir(s.getTireurId())).toList());
        return "seances";
    }

    @GetMapping("/ajouter")
    public String ajouterForm() {
        return "seance-form";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String date, @RequestParam String type,
                          @RequestParam String lieu, @RequestParam(required = false) Integer tireurId) {
        donnees.seances.add(new Seance(donnees.nouvelId(), acces.proprietaire(tireurId),
                donnees.saisonPour(date), date, type, lieu));
        return "redirect:/seances";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable int id, Model model) {
        Seance seance = ClubDonnees.trouver(donnees.seances, id);
        acces.verifierProprietaire(seance.getTireurId());
        model.addAttribute("seance", seance);
        return "seance-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable int id, @RequestParam String date,
                           @RequestParam String type, @RequestParam String lieu,
                           @RequestParam(required = false) Integer tireurId) {
        acces.verifierProprietaire(ClubDonnees.trouver(donnees.seances, id).getTireurId());
        ClubDonnees.remplacer(donnees.seances, new Seance(id, acces.proprietaire(tireurId),
                donnees.saisonPour(date), date, type, lieu));
        return "redirect:/seances";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        acces.verifierProprietaire(ClubDonnees.trouver(donnees.seances, id).getTireurId());
        donnees.supprimerSeance(id);
        return "redirect:/seances";
    }
}
