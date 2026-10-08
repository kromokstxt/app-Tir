package com.Tir.demo;

public class CategorieTir implements Identifiable {

    // Le club ne tire qu'à 300 m.
    public static final int DISTANCE = 300;

    private int id;
    private String nom;
    private int distance;

    public CategorieTir(int id, String nom, int distance) {
        this.id = id;
        this.nom = nom;
        this.distance = distance;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public int getDistance() { return distance; }
}
