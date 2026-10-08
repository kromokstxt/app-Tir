package com.Tir.demo;

import java.util.List;

public class Arme implements Identifiable {

    // Les seules catégories d'arme possibles au club (tout se tire à 300 m).
    public static final List<String> CATEGORIES = List.of("Fas 90", "Fas 57", "Mousqueton", "Fusil de sport");

    private int id;
    private String modele;
    private String categorie;
    private int tireurId;

    public Arme(int id, String modele, String categorie, int tireurId) {
        this.id = id;
        this.modele = modele;
        this.categorie = categorie;
        this.tireurId = tireurId;
    }

    public int getId() { return id; }
    public String getModele() { return modele; }
    public String getCategorie() { return categorie; }
    public int getTireurId() { return tireurId; }
}
