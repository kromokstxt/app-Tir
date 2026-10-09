package com.Tir.demo;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

// Créer son propre compte (jamais admin), avec son numéro de licence et jusqu'à 4 armes.
@Controller
public class InscriptionController {

    private final ClubDonnees donnees;
    private final PasswordEncoder encoder;
    private final Club club;

    public InscriptionController(ClubDonnees donnees, PasswordEncoder encoder, Club club) {
        this.donnees = donnees;
        this.encoder = encoder;
        this.club = club;
    }

    public static final int ARMES_MAX = 4;

    @GetMapping("/inscription")
    public String inscriptionForm(Model model) {
        preparerArmes(model, List.of(), List.of());
        return "inscription";
    }

    @PostMapping("/inscription")
    public String inscription(@RequestParam String firstName, @RequestParam String lastName,
                              @RequestParam String password,
                              @RequestParam(defaultValue = "") String licence,
                              @RequestParam(name = "categorie", required = false) List<String> categoriesSaisies,
                              @RequestParam(name = "version", required = false) List<String> versionsSaisies,
                              Model model) {
        List<String> categories = categoriesSaisies == null ? List.of() : categoriesSaisies;
        List<String> versions = versionsSaisies == null ? List.of() : versionsSaisies;
        // Une ligne d'arme compte seulement si une catégorie est choisie.
        List<Integer> armes = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            if (!categories.get(i).isBlank()) {
                armes.add(i);
            }
        }

        String erreur = null;
        if (donnees.nomPris(firstName, lastName, -1)) {
            erreur = "Un tireur porte déjà ce prénom et ce nom. Ajoutez par exemple l'initiale d'un deuxième prénom.";
        } else if (!licence.isBlank() && !Licence.numeroValide(licence)) {
            erreur = "Le numéro de licence doit faire 6 caractères.";
        } else if (armes.size() > ARMES_MAX) {
            erreur = "Au maximum " + ARMES_MAX + " armes.";
        } else if (armes.stream().anyMatch(i -> !Arme.valide(categories.get(i), version(versions, i)))) {
            erreur = "Pour un Fas 57, choisissez 02 ou 03.";
        }
        if (erreur != null) {
            model.addAttribute("erreur", erreur);
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("licence", licence);
            preparerArmes(model, categories, versions);
            return "inscription";
        }

        Shooter tireur = new Shooter(donnees.nouvelId(), firstName, lastName, club.getId(),
                donnees.nouvelIdentifiant(firstName, lastName), encoder.encode(password), false);
        donnees.tireurs.add(tireur);
        if (!licence.isBlank()) {
            donnees.licences.add(new Licence(donnees.nouvelId(), licence, tireur.getId()));
        }
        for (int i : armes) {
            donnees.armes.add(new Arme(donnees.nouvelId(), categories.get(i), version(versions, i), tireur.getId()));
        }
        return "redirect:/login?inscrit";
    }

    private static String version(List<String> versions, int i) {
        return i < versions.size() ? versions.get(i) : "";
    }

    // Les 4 lignes d'arme du formulaire, avec ce qui a déjà été choisi (si on réaffiche après une erreur).
    private void preparerArmes(Model model, List<String> categories, List<String> versions) {
        List<String[]> lignes = new ArrayList<>();
        for (int i = 0; i < ARMES_MAX; i++) {
            lignes.add(new String[] { i < categories.size() ? categories.get(i) : "", version(versions, i) });
        }
        model.addAttribute("lignesArme", lignes);
        model.addAttribute("categoriesArme", Arme.CATEGORIES);
        model.addAttribute("versions57", Arme.VERSIONS_57);
    }
}
