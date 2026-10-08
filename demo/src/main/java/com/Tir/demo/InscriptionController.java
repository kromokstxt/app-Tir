package com.Tir.demo;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Créer son propre compte (jamais admin), avec son numéro de licence si on le connaît.
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

    @GetMapping("/inscription")
    public String inscriptionForm() {
        return "inscription";
    }

    @PostMapping("/inscription")
    public String inscription(@RequestParam String firstName, @RequestParam String lastName,
                              @RequestParam String username, @RequestParam String password,
                              @RequestParam(defaultValue = "") String licence, Model model) {
        String erreur = null;
        if (donnees.tireurParUsername(username) != null) {
            erreur = "Ce nom d'utilisateur est déjà pris.";
        } else if (!licence.isBlank() && !Licence.numeroValide(licence)) {
            erreur = "Le numéro de licence doit faire 6 caractères.";
        }
        if (erreur != null) {
            model.addAttribute("erreur", erreur);
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("username", username);
            model.addAttribute("licence", licence);
            return "inscription";
        }

        Shooter tireur = new Shooter(donnees.nouvelId(), firstName, lastName, club.getId(),
                username, encoder.encode(password), false);
        donnees.tireurs.add(tireur);
        if (!licence.isBlank()) {
            donnees.licences.add(new Licence(donnees.nouvelId(), licence, "", tireur.getId()));
        }
        return "redirect:/login?inscrit";
    }
}
