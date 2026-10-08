package com.Tir.demo;

public class Evenement implements Identifiable {

    private int id;
    private String date;
    private String heure;
    private String titre;
    private String description;

    public Evenement(int id, String date, String heure, String titre, String description) {
        this.id = id;
        this.date = date;
        this.heure = heure;
        this.titre = titre;
        this.description = description;
    }

    public int getId() { return id; }
    public String getDate() { return date; }
    public String getHeure() { return heure; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
}
