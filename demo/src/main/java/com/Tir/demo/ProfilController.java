package com.Tir.demo;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Mon profil : chaque tireur voit et modifie le sien.
@Controller
public class ProfilController {

    private final ClubDonnees donnees;
    private final Acces acces;
    private final PasswordEncoder encoder;

    public ProfilController(ClubDonnees donnees, Acces acces, PasswordEncoder encoder) {
        this.donnees = donnees;
        this.acces = acces;
        this.encoder = encoder;
    }

    @GetMapping("/profil")
    public String profil() {
        return "profil";
    }

    @GetMapping("/profil/modifier")
    public String modifierForm() {
        return "profil-form";
    }

    @PostMapping("/profil/modifier")
    public String modifier(@RequestParam String firstName, @RequestParam String lastName,
                           @RequestParam(defaultValue = "") String password, Model model) {
        Shooter moi = acces.moi();
        if (donnees.nomPris(firstName, lastName, moi.getId())) {
            model.addAttribute("erreur", "Un tireur porte déjà ce prénom et ce nom.");
            return "profil-form";
        }
        String motDePasse = password.isBlank() ? moi.getPassword() : encoder.encode(password);
        ClubDonnees.remplacer(donnees.tireurs, new Shooter(moi.getId(), firstName, lastName,
                moi.getClubId(), moi.getUsername(), motDePasse, moi.isAdmin()));
        return "redirect:/profil";
    }
}
