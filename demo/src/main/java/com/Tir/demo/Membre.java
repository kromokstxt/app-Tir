package com.Tir.demo;

public class Membre {
    public int id;
    private String nom;
    private String prenom;




    public Membre(int id, String nom, String prenom) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
    }

    public int getId() {return id; }
    public String getNom() {return nom; }
    public String getPrenom() {return prenom; }
   

    
}
