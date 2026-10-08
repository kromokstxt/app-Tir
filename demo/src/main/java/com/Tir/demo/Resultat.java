package com.Tir.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Une feuille de résultat : la liste des coups, notés de 0 à 10 ou de 0 à 100 (M = manqué).
public class Resultat implements Identifiable {

    public static final String MANQUE = "M";

    private int id;
    private int score;
    private String date;
    private int seanceId;
    private int categorieId;
    private int echelle;
    private List<String> coups;

    public Resultat(int id, String date, int seanceId, int categorieId, int echelle, List<String> coups) {
        this.id = id;
        this.date = date;
        this.seanceId = seanceId;
        this.categorieId = categorieId;
        this.echelle = echelle;
        this.coups = List.copyOf(coups);
        this.score = coups.stream().filter(c -> !c.equals(MANQUE)).mapToInt(Integer::parseInt).sum();
    }

    // Lit les coups tapés dans le formulaire, par ex. « 10 9 M 8 » ou « 95, 87, M ».
    // Renvoie null si un coup n'est pas valable pour cette échelle.
    public static List<String> lireCoups(String texte, int echelle) {
        List<String> coups = new ArrayList<>();
        for (String coup : Arrays.asList(texte.trim().split("[\\s,;]+"))) {
            if (coup.isEmpty()) {
                continue;
            }
            if (coup.equalsIgnoreCase(MANQUE)) {
                coups.add(MANQUE);
                continue;
            }
            try {
                int valeur = Integer.parseInt(coup);
                if (valeur < 0 || valeur > echelle) {
                    return null;
                }
                coups.add(String.valueOf(valeur));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return coups.isEmpty() ? null : coups;
    }

    public int getId() { return id; }
    public int getScore() { return score; }
    public String getDate() { return date; }
    public int getSeanceId() { return seanceId; }
    public int getCategorieId() { return categorieId; }
    public int getEchelle() { return echelle; }
    public List<String> getCoups() { return coups; }
    public int getScoreMax() { return echelle * coups.size(); }
    public String getCoupsTexte() { return String.join(" ", coups); }
}
