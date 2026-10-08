package com.Tir.demo;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;

// Ajoute à toutes les pages : le club, le tireur connecté (« moi ») et s'il est admin.
@ControllerAdvice
public class ModeleCommun {

    private final Club club;
    private final ClubDonnees donnees;

    public ModeleCommun(Club club, ClubDonnees donnees) {
        this.club = club;
        this.donnees = donnees;
    }

    @ModelAttribute("club")
    public Club club() {
        return club;
    }

    @ModelAttribute("moi")
    public Shooter moi() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return donnees.tireurParUsername(auth.getName());
    }

    // La date du jour, pour pré-remplir les champs de date (format 2026-10-08).
    @ModelAttribute("aujourdhui")
    public String aujourdhui() {
        return LocalDate.now().toString();
    }

    @ModelAttribute("admin")
    public boolean admin() {
        Shooter moi = moi();
        return moi != null && moi.isAdmin();
    }
}
