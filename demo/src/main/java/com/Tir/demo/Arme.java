package com.Tir.demo;

public class Arme {

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
