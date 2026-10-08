package com.Tir.demo;

public class Saison implements Identifiable {

    private int id;
    private String annee;
    private String dateDebut;
    private String dateFin;

    public Saison(int id, String annee, String dateDebut, String dateFin) {
        this.id = id;
        this.annee = annee;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    public int getId() { return id; }
    public String getAnnee() { return annee; }
    public String getDateDebut() { return dateDebut; }
    public String getDateFin() { return dateFin; }
}
