package com.Tir.demo;

// Une licence n'expire pas : seulement son numéro (celui du Polytronic au stand).
public class Licence implements Identifiable {

    private int id;
    private String numero;
    private int tireurId;

    public Licence(int id, String numero, int tireurId) {
        this.id = id;
        this.numero = numero;
        this.tireurId = tireurId;
    }

    // Le numéro de licence fait exactement 6 caractères.
    public static boolean numeroValide(String numero) {
        return numero != null && numero.length() == 6;
    }

    public int getId() { return id; }
    public String getNumero() { return numero; }
    public int getTireurId() { return tireurId; }
}
