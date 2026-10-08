package com.Tir.demo;

public class Club implements Identifiable {

    private int id;
    private String nom;

    public Club(int id, String nom) {
        this.id = id;
        this.nom = nom;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
}
