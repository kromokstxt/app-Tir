package com.Tir.demo;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

// Une table de la base : comment lire ses lignes, et comment y écrire, ajouter, modifier ou effacer un élément.
// Les colonnes sont données sans « id », qui est toujours la première.
public class Table<T extends Identifiable> {

    private final JdbcTemplate jdbc;
    private final String nom;
    private final List<String> colonnes;
    private final RowMapper<T> lecture;
    private final Function<T, List<Object>> valeurs;

    public Table(JdbcTemplate jdbc, String nom, List<String> colonnes,
                 RowMapper<T> lecture, Function<T, List<Object>> valeurs) {
        this.jdbc = jdbc;
        this.nom = nom;
        this.colonnes = colonnes;
        this.lecture = lecture;
        this.valeurs = valeurs;
    }

    public List<T> toutLire() {
        return jdbc.query("SELECT * FROM " + nom + " ORDER BY id", lecture);
    }

    public int plusGrandId() {
        Integer max = jdbc.queryForObject("SELECT MAX(id) FROM " + nom, Integer.class);
        return max == null ? 0 : max;
    }

    public void inserer(T element) {
        String points = String.join(", ", Collections.nCopies(colonnes.size() + 1, "?"));
        jdbc.update("INSERT INTO " + nom + " (id, " + String.join(", ", colonnes) + ") VALUES (" + points + ")",
                avecId(element, true));
    }

    public void modifier(T element) {
        String affectations = String.join(", ", colonnes.stream().map(c -> c + " = ?").toList());
        jdbc.update("UPDATE " + nom + " SET " + affectations + " WHERE id = ?", avecId(element, false));
    }

    public void supprimer(int id) {
        jdbc.update("DELETE FROM " + nom + " WHERE id = ?", id);
    }

    // Les valeurs de l'élément, avec son id au début (insertion) ou à la fin (modification).
    private Object[] avecId(T element, boolean auDebut) {
        List<Object> v = valeurs.apply(element);
        Object[] tout = new Object[v.size() + 1];
        int decalage = auDebut ? 1 : 0;
        for (int i = 0; i < v.size(); i++) {
            tout[i + decalage] = v.get(i);
        }
        tout[auDebut ? 0 : v.size()] = element.getId();
        return tout;
    }

    // Les vrai/faux sont gardés en 1/0.
    public static int nombre(boolean valeur) {
        return valeur ? 1 : 0;
    }
}
