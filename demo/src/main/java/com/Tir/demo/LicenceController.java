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

@Controller
@RequestMapping("/licences")
public class LicenceController {

    private final ClubDonnees donnees;
    private final Acces acces;

    public LicenceController(ClubDonnees donnees, Acces acces) {
        this.donnees = donnees;
        this.acces = acces;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("licences", donnees.licences.stream().filter(l -> acces.peutVoir(l.getTireurId())).toList());
        return "licences";
    }

    @GetMapping("/ajouter")
    public String ajouterForm() {
        return "licence-form";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String numero, @RequestParam(defaultValue = "") String dateValidite,
                          @RequestParam(required = false) Integer tireurId) {
        verifierNumero(numero);
        donnees.licences.add(new Licence(donnees.nouvelId(), numero, dateValidite, acces.proprietaire(tireurId)));
        return "redirect:/licences";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable int id, Model model) {
        Licence licence = ClubDonnees.trouver(donnees.licences, id);
        acces.verifierProprietaire(licence.getTireurId());
        model.addAttribute("licence", licence);
        return "licence-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable int id, @RequestParam String numero, @RequestParam(defaultValue = "") String dateValidite,
                           @RequestParam(required = false) Integer tireurId) {
        acces.verifierProprietaire(ClubDonnees.trouver(donnees.licences, id).getTireurId());
        verifierNumero(numero);
        ClubDonnees.remplacer(donnees.licences, new Licence(id, numero, dateValidite, acces.proprietaire(tireurId)));
        return "redirect:/licences";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable int id) {
        acces.verifierProprietaire(ClubDonnees.trouver(donnees.licences, id).getTireurId());
        ClubDonnees.supprimer(donnees.licences, id);
        return "redirect:/licences";
    }

    private void verifierNumero(String numero) {
        if (!Licence.numeroValide(numero)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le numéro de licence doit faire 6 caractères");
        }
    }
}
