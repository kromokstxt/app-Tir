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

// « Entrer un tir » et « Mes résultats ». Un tireur ne voit que ses tirs, l'admin voit tout.
// La saison d'un tir est trouvée automatiquement d'après sa date.
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
        // Les plus récents en premier.
        List<Resultat> resultats = donnees.resultats.stream()
                .filter(r -> acces.peutVoir(r.getTireurId()))
                .sorted(Comparator.comparing(Resultat::getDate).reversed())
                .toList();
        model.addAttribute("resultats", resultats);
        model.addAttribute("moyenne", resultats.stream().mapToDouble(Resultat::getNoteSur100).average().orElse(-1));
        return "resultats";
    }

    @GetMapping("/ajouter")
    public String ajouterForm(Model model) {
        model.addAttribute("categorieParDefaut", categorieParDefaut());
        return "resultat-form";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String date, @RequestParam int categorieId,
                          @RequestParam(defaultValue = "false") boolean externe,
                          @RequestParam(defaultValue = "false") boolean coupsProfonds,
                          @RequestParam String coups, @RequestParam(required = false) Integer tireurId) {
        ClubDonnees.trouver(donnees.categories, categorieId);
        Resultat resultat = new Resultat(donnees.nouvelId(), acces.proprietaire(tireurId), donnees.saisonPour(date),
                date, categorieId, externe, coupsProfonds, lireCoups(coups, coupsProfonds));
        donnees.resultats.add(resultat);
        return "redirect:/resultats/" + resultat.getId();
    }

    // Voir un tir sur la cible.
    @GetMapping("/{id}")
    public String voir(@PathVariable int id, Model model) {
        Resultat resultat = ClubDonnees.trouver(donnees.resultats, id);
        acces.verifierProprietaire(resultat.getTireurId());
        model.addAttribute("resultat", resultat);
        return "resultat";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable int id, Model model) {
        Resultat resultat = ClubDonnees.trouver(donnees.resultats, id);
        acces.verifierProprietaire(resultat.getTireurId());
        model.addAttribute("resultat", resultat);
        model.addAttribute("categorieParDefaut", resultat.getCategorieId());
        return "resultat-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable int id, @RequestParam String date, @RequestParam int categorieId,
                           @RequestParam(defaultValue = "false") boolean externe,
                           @RequestParam(defaultValue = "false") boolean coupsProfonds,
                           @RequestParam String coups, @RequestParam(required = false) Integer tireurId) {
        acces.verifierProprietaire(ClubDonnees.trouver(donnees.resultats, id).getTireurId());
        ClubDonnees.trouver(donnees.categories, categorieId);
        ClubDonnees.remplacer(donnees.resultats, new Resultat(id, acces.proprietaire(tireurId), donnees.saisonPour(date),
                date, categorieId, externe, coupsProfonds, lireCoups(coups, coupsProfonds)));
        return "redirect:/resultats/" + id;
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        acces.verifierProprietaire(ClubDonnees.trouver(donnees.resultats, id).getTireurId());
        ClubDonnees.supprimer(donnees.resultats, id);
        return "redirect:/resultats";
    }

    // L'arme proposée d'office : la catégorie de la première arme du tireur, sinon la première catégorie.
    private int categorieParDefaut() {
        int moi = acces.moi().getId();
        return donnees.armes.stream().filter(a -> a.getTireurId() == moi).map(Arme::getCategorie)
                .flatMap(nom -> donnees.categories.stream().filter(c -> c.getNom().equals(nom)))
                .mapToInt(CategorieTir::getId).findFirst()
                .orElse(donnees.categories.isEmpty() ? 0 : donnees.categories.get(0).getId());
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
