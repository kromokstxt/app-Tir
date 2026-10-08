package com.Tir.demo;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

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

    @ModelAttribute("admin")
    public boolean admin() {
        Shooter moi = moi();
        return moi != null && moi.isAdmin();
    }
}
