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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
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

    // Une période pour la moyenne : son nom dans l'adresse, son titre, sa moyenne et son nombre de tirs.
    public record Periode(String code, String titre, double moyenne, int nombreDeTirs, boolean choisie) {}

    // La moyenne sur 100 d'aujourd'hui, du mois, de l'année et depuis toujours,
    // avec tous les coups de la période choisie réunis sur une seule cible.
    @GetMapping("/moyenne")
    public String moyenne(@RequestParam(defaultValue = "tout") String periode, Model model) {
        String aujourdhui = LocalDate.now().toString();
        // Les dates sont au format 2026-10-16 : « ce mois » = commence par 2026-10, « cette année » = par 2026.
        Map<String, String> debuts = new LinkedHashMap<>();
        debuts.put("jour", aujourdhui);
        debuts.put("mois", aujourdhui.substring(0, 7));
        debuts.put("annee", aujourdhui.substring(0, 4));
        debuts.put("tout", "");
        Map<String, String> titres = Map.of("jour", "Aujourd'hui", "mois", "Ce mois", "annee", "Cette année", "tout", "Depuis toujours");
        if (!debuts.containsKey(periode)) {
            periode = "tout";
        }

        List<Resultat> visibles = donnees.resultats.stream().filter(r -> acces.peutVoir(r.getTireurId())).toList();
        List<Periode> periodes = new ArrayList<>();
        List<Resultat> choisis = List.of();
        for (Map.Entry<String, String> debut : debuts.entrySet()) {
            List<Resultat> tirs = visibles.stream().filter(r -> r.getDate().startsWith(debut.getValue())).toList();
            boolean choisie = debut.getKey().equals(periode);
            if (choisie) {
                choisis = tirs;
            }
            periodes.add(new Periode(debut.getKey(), titres.get(debut.getKey()),
                    tirs.stream().mapToDouble(Resultat::getNoteSur100).average().orElse(-1), tirs.size(), choisie));
        }
        model.addAttribute("periodes", periodes);
        model.addAttribute("periodeChoisie", periodes.stream().filter(Periode::choisie).findFirst().orElseThrow());
        // Tous les coups de la période, pour les montrer ensemble sur la cible.
        model.addAttribute("tousLesCoups", choisis.stream().map(Resultat::getCoupsTexte).collect(Collectors.joining(" ")));
        model.addAttribute("nombreDeCoups", choisis.stream().mapToInt(r -> r.getCoups().size()).sum());
        return "moyenne";
    }

    @GetMapping("/ajouter")
    public String ajouterForm(Model model) {
        model.addAttribute("armes", armesDe(acces.moi().getId()));
        return "resultat-form";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String date, @RequestParam int armeId,
                          @RequestParam(defaultValue = "false") boolean externe,
                          @RequestParam(defaultValue = "false") boolean coupsProfonds,
                          @RequestParam String coups) {
        // On entre toujours un tir pour soi, avec une de ses armes (l'admin aussi).
        Arme arme = armeDe(acces.moi().getId(), armeId);
        Resultat resultat = new Resultat(donnees.nouvelId(), arme.getTireurId(), donnees.saisonPour(date),
                date, arme.getNom(), externe, coupsProfonds, lireCoups(coups, coupsProfonds));
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
        model.addAttribute("armes", armesDe(resultat.getTireurId()));
        return "resultat-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable int id, @RequestParam String date, @RequestParam int armeId,
                           @RequestParam(defaultValue = "false") boolean externe,
                           @RequestParam(defaultValue = "false") boolean coupsProfonds,
                           @RequestParam String coups) {
        Resultat ancien = ClubDonnees.trouver(donnees.resultats, id);
        acces.verifierProprietaire(ancien.getTireurId());
        // Le tir reste au même tireur, avec une de ses armes.
        Arme arme = armeDe(ancien.getTireurId(), armeId);
        ClubDonnees.remplacer(donnees.resultats, new Resultat(id, arme.getTireurId(), donnees.saisonPour(date),
                date, arme.getNom(), externe, coupsProfonds, lireCoups(coups, coupsProfonds)));
        return "redirect:/resultats/" + id;
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        acces.verifierProprietaire(ClubDonnees.trouver(donnees.resultats, id).getTireurId());
        ClubDonnees.supprimer(donnees.resultats, id);
        return "redirect:/resultats";
    }

    // On tire avec une de ses armes (celles du profil).
    private List<Arme> armesDe(int tireurId) {
        return donnees.armes.stream().filter(a -> a.getTireurId() == tireurId).toList();
    }

    // L'arme choisie doit appartenir à ce tireur, sinon c'est refusé.
    private Arme armeDe(int tireurId, int armeId) {
        Arme arme = ClubDonnees.trouver(donnees.armes, armeId);
        if (arme.getTireurId() != tireurId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return arme;
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
