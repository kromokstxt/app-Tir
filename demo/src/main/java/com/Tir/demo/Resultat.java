package com.Tir.demo;

public class Resultat implements Identifiable {

    private int id;
    private int score;
    private String date;
    private int seanceId;
    private int categorieId;

    public Resultat(int id, int score, String date, int seanceId, int categorieId) {
        this.id = id;
        this.score = score;
        this.date = date;
        this.seanceId = seanceId;
        this.categorieId = categorieId;
    }

    public int getId() { return id; }
    public int getScore() { return score; }
    public String getDate() { return date; }
    public int getSeanceId() { return seanceId; }
    public int getCategorieId() { return categorieId; }
}
