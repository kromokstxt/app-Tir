package com.Tir.demo;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Gestion de tous les tireurs : réservé à l'admin.
@Controller
public class ShooterController {

    private final ClubDonnees donnees;
    private final Acces acces;
    private final PasswordEncoder encoder;
    private final Club club;

    public ShooterController(ClubDonnees donnees, Acces acces, PasswordEncoder encoder, Club club) {
        this.donnees = donnees;
        this.acces = acces;
        this.encoder = encoder;
        this.club = club;
    }

    @GetMapping("/tireurs")
    public String listShooters(Model model) {
        acces.verifierAdmin();
        model.addAttribute("shooters", donnees.tireurs);
        return "tireurs";
    }

    @GetMapping("/tireurs/ajouter")
    public String showAddForm() {
        acces.verifierAdmin();
        return "tireur-form";
    }

    @PostMapping("/tireurs/ajouter")
    public String addShooter(@RequestParam String firstName, @RequestParam String lastName,
                             @RequestParam String password,
                             @RequestParam(defaultValue = "false") boolean admin, Model model) {
        acces.verifierAdmin();
        if (donnees.nomPris(firstName, lastName, -1)) {
            model.addAttribute("erreur", "Un tireur porte déjà ce prénom et ce nom.");
            return "tireur-form";
        }
        donnees.tireurs.add(new Shooter(donnees.nouvelId(), firstName, lastName, club.getId(),
                donnees.nouvelIdentifiant(firstName, lastName), encoder.encode(password), admin));
        return "redirect:/tireurs";
    }

    @GetMapping("/tireurs/{id}/modifier")
    public String showEditForm(@PathVariable int id, Model model) {
        acces.verifierAdmin();
        model.addAttribute("tireur", ClubDonnees.trouver(donnees.tireurs, id));
        return "tireur-form";
    }

    @PostMapping("/tireurs/{id}/modifier")
    public String editShooter(@PathVariable int id, @RequestParam String firstName, @RequestParam String lastName,
                              @RequestParam(defaultValue = "") String password,
                              @RequestParam(defaultValue = "false") boolean admin, Model model) {
        acces.verifierAdmin();
        Shooter ancien = ClubDonnees.trouver(donnees.tireurs, id);
        // L'admin ne peut pas se retirer lui-même les droits admin.
        if (id == acces.moi().getId()) {
            admin = true;
        }
        if (donnees.nomPris(firstName, lastName, id)) {
            model.addAttribute("tireur", ancien);
            model.addAttribute("erreur", "Un tireur porte déjà ce prénom et ce nom.");
            return "tireur-form";
        }
        String motDePasse = password.isBlank() ? ancien.getPassword() : encoder.encode(password);
        ClubDonnees.remplacer(donnees.tireurs, new Shooter(id, firstName, lastName, ancien.getClubId(),
                ancien.getUsername(), motDePasse, admin));
        return "redirect:/tireurs";
    }

    @PostMapping("/tireurs/{id}/supprimer")
    public String deleteShooter(@PathVariable int id) {
        acces.verifierAdmin();
        if (id != acces.moi().getId()) {
            donnees.supprimerTireur(id);
        }
        return "redirect:/tireurs";
    }
}
