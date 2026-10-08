package com.Tir.demo;

public class Annonce implements Identifiable {

    private int id;
    private String titre;
    private String message;
    private String date;

    public Annonce(int id, String titre, String message, String date) {
        this.id = id;
        this.titre = titre;
        this.message = message;
        this.date = date;
    }

    public int getId() { return id; }
    public String getTitre() { return titre; }
    public String getMessage() { return message; }
    public String getDate() { return date; }
}
