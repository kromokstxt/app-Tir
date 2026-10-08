package com.Tir.demo;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// Qui est connecté, et ce qu'il a le droit de faire :
// un tireur ne voit et ne modifie que ses propres données, l'admin voit et modifie tout.
@Component
public class Acces {

    private final ClubDonnees donnees;

    public Acces(ClubDonnees donnees) {
        this.donnees = donnees;
    }

    public Shooter moi() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Shooter moi = auth == null ? null : donnees.tireurParUsername(auth.getName());
        if (moi == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return moi;
    }

    public boolean estAdmin() {
        return moi().isAdmin();
    }

    public void verifierAdmin() {
        if (!estAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public boolean peutVoir(int tireurId) {
        return estAdmin() || moi().getId() == tireurId;
    }

    public void verifierProprietaire(int tireurId) {
        if (!peutVoir(tireurId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    // Le tireur à qui rattacher un élément : l'admin peut choisir, les autres c'est toujours eux.
    public int proprietaire(Integer tireurIdChoisi) {
        if (estAdmin() && tireurIdChoisi != null) {
            return ClubDonnees.trouver(donnees.tireurs, tireurIdChoisi).getId();
        }
        return moi().getId();
    }
}
