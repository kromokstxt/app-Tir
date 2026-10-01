package com.Tir.demo;

public class MembreComite {

    private int id;
    private String prenom;
    private String nom;
    private String fonction;
    private int clubId;

    public MembreComite(int id, String prenom, String nom, String fonction, int clubId) {
        this.id = id;
        this.prenom = prenom;
        this.nom = nom;
        this.fonction = fonction;
        this.clubId = clubId;
    }

    public int getId() { return id; }
    public String getPrenom() { return prenom; }
    public String getNom() { return nom; }
    public String getFonction() { return fonction; }
    public int getClubId() { return clubId; }
}
