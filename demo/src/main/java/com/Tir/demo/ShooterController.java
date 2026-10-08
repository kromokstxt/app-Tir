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
                             @RequestParam String username, @RequestParam String password,
                             @RequestParam(defaultValue = "false") boolean admin, Model model) {
        acces.verifierAdmin();
        if (donnees.tireurParUsername(username) != null) {
            model.addAttribute("erreur", "Ce nom d'utilisateur est déjà pris.");
            return "tireur-form";
        }
        donnees.tireurs.add(new Shooter(donnees.nouvelId(), firstName, lastName, club.getId(),
                username, encoder.encode(password), admin));
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
                              @RequestParam String username, @RequestParam(defaultValue = "") String password,
                              @RequestParam(defaultValue = "false") boolean admin, Model model) {
        acces.verifierAdmin();
        Shooter ancien = ClubDonnees.trouver(donnees.tireurs, id);
        // L'admin ne peut pas changer son propre identifiant ni se retirer les droits admin.
        if (id == acces.moi().getId()) {
            username = ancien.getUsername();
            admin = true;
        }
        Shooter autre = donnees.tireurParUsername(username);
        if (autre != null && autre.getId() != id) {
            model.addAttribute("tireur", ancien);
            model.addAttribute("erreur", "Ce nom d'utilisateur est déjà pris.");
            return "tireur-form";
        }
        String motDePasse = password.isBlank() ? ancien.getPassword() : encoder.encode(password);
        ClubDonnees.remplacer(donnees.tireurs, new Shooter(id, firstName, lastName, ancien.getClubId(),
                username, motDePasse, admin));
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
