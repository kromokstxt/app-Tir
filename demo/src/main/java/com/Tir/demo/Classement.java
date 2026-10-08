package com.Tir.demo;

public class Classement implements Identifiable {

    private int id;
    private int position;
    private int points;
    private int saisonId;
    private int tireurId;

    public Classement(int id, int position, int points, int saisonId, int tireurId) {
        this.id = id;
        this.position = position;
        this.points = points;
        this.saisonId = saisonId;
        this.tireurId = tireurId;
    }

    public int getId() { return id; }
    public int getPosition() { return position; }
    public int getPoints() { return points; }
    public int getSaisonId() { return saisonId; }
    public int getTireurId() { return tireurId; }
}
