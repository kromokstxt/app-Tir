package com.Tir.demo;

public class Licence implements Identifiable {

    private int id;
    private String numero;
    private String dateValidite;
    private int tireurId;

    public Licence(int id, String numero, String dateValidite, int tireurId) {
        this.id = id;
        this.numero = numero;
        this.dateValidite = dateValidite;
        this.tireurId = tireurId;
    }

    // Le numéro de licence fait exactement 6 caractères.
    public static boolean numeroValide(String numero) {
        return numero != null && numero.length() == 6;
    }

    public int getId() { return id; }
    public String getNumero() { return numero; }
    public String getDateValidite() { return dateValidite; }
    public int getTireurId() { return tireurId; }
}
