package com.Tir.demo;

public class Seance {

    private int id;
    private int tireurId;
    private int saisonId;
    private String date;
    private String type;
    private String lieu;

    public Seance(int id, int tireurId, int saisonId, String date, String type, String lieu) {
        this.id = id;
        this.tireurId = tireurId;
        this.saisonId = saisonId;
        this.date = date;
        this.type = type;
        this.lieu = lieu;
    }

    public int getId() { return id; }
    public int getTireurId() { return tireurId; }
    public int getSaisonId() { return saisonId; }
    public String getDate() { return date; }
    public String getType() { return type; }
    public String getLieu() { return lieu; }
}
