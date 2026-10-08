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

import java.util.Comparator;
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
    public String ajouter(@RequestParam String date, @RequestParam int seanceId, @RequestParam int categorieId,
                          @RequestParam(defaultValue = "false") boolean coupsProfonds, @RequestParam String coups) {
        verifierSeanceEtCategorie(seanceId, categorieId);
        donnees.resultats.add(new Resultat(donnees.nouvelId(), date, seanceId, categorieId, coupsProfonds, lireCoups(coups, coupsProfonds)));
        return "redirect:/resultats";
    }

    // Voir la feuille de résultat sur la cible.
    @GetMapping("/{id}")
    public String voir(@PathVariable int id, Model model) {
        Resultat resultat = ClubDonnees.trouver(donnees.resultats, id);
        acces.verifierProprietaire(donnees.proprietaireSeance(resultat.getSeanceId()));
        model.addAttribute("resultat", resultat);
        return "resultat";
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
    public String modifier(@PathVariable int id, @RequestParam String date, @RequestParam int seanceId,
                           @RequestParam int categorieId, @RequestParam(defaultValue = "false") boolean coupsProfonds, @RequestParam String coups) {
        Resultat ancien = ClubDonnees.trouver(donnees.resultats, id);
        acces.verifierProprietaire(donnees.proprietaireSeance(ancien.getSeanceId()));
        verifierSeanceEtCategorie(seanceId, categorieId);
        ClubDonnees.remplacer(donnees.resultats, new Resultat(id, date, seanceId, categorieId, coupsProfonds, lireCoups(coups, coupsProfonds)));
        return "redirect:/resultats/" + id;
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        Resultat resultat = ClubDonnees.trouver(donnees.resultats, id);
        acces.verifierProprietaire(donnees.proprietaireSeance(resultat.getSeanceId()));
        ClubDonnees.supprimer(donnees.resultats, id);
        return "redirect:/resultats";
    }

    private List<Seance> mesSeances() {
        // Les plus récentes en premier : la séance du jour est choisie d'office.
        return donnees.seances.stream().filter(s -> acces.peutVoir(s.getTireurId()))
                .sorted(Comparator.comparing(Seance::getDate).reversed()).toList();
    }

    private void verifierSeanceEtCategorie(int seanceId, int categorieId) {
        acces.verifierProprietaire(ClubDonnees.trouver(donnees.seances, seanceId).getTireurId());
        ClubDonnees.trouver(donnees.categories, categorieId);
    }

    private List<String> lireCoups(String texte, boolean coupsProfonds) {
        List<String> coups = Resultat.lireCoups(texte, coupsProfonds);
        if (coups == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, coupsProfonds
                    ? "Chaque coup doit avoir son coup profond (0 à 100), ou être M"
                    : "Chaque coup doit avoir ses points (0 à 10), ou être M");
        }
        return coups;
    }
}
