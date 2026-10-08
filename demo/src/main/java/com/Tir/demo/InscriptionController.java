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
                              @RequestParam String username, @RequestParam String password,
                              @RequestParam(defaultValue = "") String licence,
                              @RequestParam(name = "modele", required = false) List<String> modelesSaisis,
                              @RequestParam(name = "categorie", required = false) List<String> categoriesSaisies,
                              Model model) {
        List<String> modeles = modelesSaisis == null ? List.of() : modelesSaisis;
        List<String> categories = categoriesSaisies == null ? List.of() : categoriesSaisies;
        // Une arme compte seulement si son modèle est rempli.
        List<Integer> armes = new ArrayList<>();
        for (int i = 0; i < modeles.size(); i++) {
            if (!modeles.get(i).isBlank()) {
                armes.add(i);
            }
        }

        String erreur = null;
        if (donnees.tireurParUsername(username) != null) {
            erreur = "Ce nom d'utilisateur est déjà pris.";
        } else if (!licence.isBlank() && !Licence.numeroValide(licence)) {
            erreur = "Le numéro de licence doit faire 6 caractères.";
        } else if (armes.size() > ARMES_MAX) {
            erreur = "Au maximum " + ARMES_MAX + " armes.";
        } else if (armes.stream().anyMatch(i -> i >= categories.size() || !Arme.CATEGORIES.contains(categories.get(i)))) {
            erreur = "Choisissez une catégorie pour chaque arme.";
        }
        if (erreur != null) {
            model.addAttribute("erreur", erreur);
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("username", username);
            model.addAttribute("licence", licence);
            preparerArmes(model, modeles, categories);
            return "inscription";
        }

        Shooter tireur = new Shooter(donnees.nouvelId(), firstName, lastName, club.getId(),
                username, encoder.encode(password), false);
        donnees.tireurs.add(tireur);
        if (!licence.isBlank()) {
            donnees.licences.add(new Licence(donnees.nouvelId(), licence, "", tireur.getId()));
        }
        for (int i : armes) {
            donnees.armes.add(new Arme(donnees.nouvelId(), modeles.get(i).trim(), categories.get(i), tireur.getId()));
        }
        return "redirect:/login?inscrit";
    }

    // Les 4 lignes d'arme du formulaire, avec ce qui a déjà été tapé (si on réaffiche après une erreur).
    private void preparerArmes(Model model, List<String> modeles, List<String> categories) {
        List<String[]> lignes = new ArrayList<>();
        for (int i = 0; i < ARMES_MAX; i++) {
            lignes.add(new String[] {
                    i < modeles.size() ? modeles.get(i) : "",
                    i < categories.size() ? categories.get(i) : ""
            });
        }
        model.addAttribute("lignesArme", lignes);
        model.addAttribute("categoriesArme", Arme.CATEGORIES);
    }
}
