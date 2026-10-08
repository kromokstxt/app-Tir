package com.Tir.demo;

import java.util.List;

// Une arme : sa catégorie, et pour un Fas 57 sa version (02 ou 03).
public class Arme implements Identifiable {

    // Les seules catégories d'arme possibles au club (tout se tire à 300 m).
    public static final String FAS_57 = "Fas 57";
    public static final List<String> CATEGORIES = List.of("Fas 90", FAS_57, "Mousqueton", "Fusil de sport");
    public static final List<String> VERSIONS_57 = List.of("02", "03");

    private int id;
    private String categorie;
    private String version;
    private int tireurId;

    public Arme(int id, String categorie, String version, int tireurId) {
        this.id = id;
        this.categorie = categorie;
        this.version = FAS_57.equals(categorie) ? version : "";
        this.tireurId = tireurId;
    }

    // Vrai si la catégorie existe et, pour un Fas 57, si la version est 02 ou 03.
    public static boolean valide(String categorie, String version) {
        if (!CATEGORIES.contains(categorie)) {
            return false;
        }
        return !FAS_57.equals(categorie) || VERSIONS_57.contains(version);
    }

    public int getId() { return id; }
    public String getCategorie() { return categorie; }
    public String getVersion() { return version; }
    public int getTireurId() { return tireurId; }

    // « Fas 57/03 », « Fas 90 », …
    public String getNom() {
        return version.isEmpty() ? categorie : categorie + "/" + version;
    }
}
