package com.Tir.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Un tir : qui a tiré, quand, avec quelle arme, au stand ou en tir externe,
// et la feuille de résultat. Chaque coup a ses points (0 à 10, ou M = manqué) et la
// direction de sa flèche (H = haut, BD = bas-droite, …), facultative.
// Avec l'option « coups profonds », on donne le coup profond de chaque coup
// (0 à 100, 100 = plein centre) et ses points en sont déduits.
//
// Un coup est gardé sous forme de texte : « 9>HD » (9, en haut à droite),
// « 9:87>HD » (coup profond 87, donc 9 points), « 10 » (sans flèche) ou « M ».
public class Resultat implements Identifiable {

    public static final String MANQUE = "M";
    public static final List<String> DIRECTIONS = List.of("H", "HD", "D", "BD", "B", "BG", "G", "HG");
    public static final Map<String, String> FLECHES = Map.of(
            "H", "↑", "HD", "↗", "D", "→", "BD", "↘", "B", "↓", "BG", "↙", "G", "←", "HG", "↖");

    private static final Pattern COUP = Pattern.compile("(\\d{1,2})?(?::(\\d{1,3}))?(?:>([A-Z]{1,2}))?");

    private int id;
    private int tireurId;
    private int saisonId;
    private String date;
    private int categorieId;
    private boolean externe;
    private boolean coupsProfonds;
    private List<String> coups;
    private int score;
    private int scoreProfond;

    public Resultat(int id, int tireurId, int saisonId, String date, int categorieId,
                    boolean externe, boolean coupsProfonds, List<String> coups) {
        this.id = id;
        this.tireurId = tireurId;
        this.saisonId = saisonId;
        this.date = date;
        this.categorieId = categorieId;
        this.externe = externe;
        this.coupsProfonds = coupsProfonds;
        this.coups = List.copyOf(coups);
        for (String coup : coups) {
            if (!coup.equals(MANQUE)) {
                Matcher m = COUP.matcher(coup);
                m.matches();
                score += Integer.parseInt(m.group(1));
                scoreProfond += m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
            }
        }
    }

    // Les points d'un coup profond : la cible est découpée en 10 anneaux égaux,
    // 100 = plein centre. 91 à 100 → 10, 81 à 90 → 9, …, 1 à 10 → 1, 0 → 0.
    public static int pointsDepuisProfond(int profond) {
        return (profond + 9) / 10;
    }

    // Lit les coups envoyés par le formulaire, séparés par des espaces.
    // Avec les coups profonds, chaque coup touché doit avoir son coup profond et ses points
    // en sont déduits ; sans, chaque coup doit avoir ses points et un coup profond est ignoré.
    // Renvoie null si un coup n'est pas valable.
    public static List<String> lireCoups(String texte, boolean coupsProfonds) {
        List<String> coups = new ArrayList<>();
        for (String coup : texte.trim().split("\\s+")) {
            if (coup.isEmpty()) {
                continue;
            }
            if (coup.equalsIgnoreCase(MANQUE)) {
                coups.add(MANQUE);
                continue;
            }
            Matcher m = COUP.matcher(coup.toUpperCase());
            if (!m.matches()) {
                return null;
            }
            String direction = m.group(3);
            if (direction != null && !DIRECTIONS.contains(direction)) {
                return null;
            }
            String propre;
            if (coupsProfonds) {
                if (m.group(2) == null || Integer.parseInt(m.group(2)) > 100) {
                    return null;
                }
                int profond = Integer.parseInt(m.group(2));
                propre = pointsDepuisProfond(profond) + ":" + profond;
            } else {
                if (m.group(1) == null || Integer.parseInt(m.group(1)) > 10) {
                    return null;
                }
                propre = String.valueOf(Integer.parseInt(m.group(1)));
            }
            if (direction != null) {
                propre += ">" + direction;
            }
            coups.add(propre);
        }
        return coups.isEmpty() ? null : coups;
    }

    public int getId() { return id; }
    public int getTireurId() { return tireurId; }
    public int getSaisonId() { return saisonId; }
    public String getDate() { return date; }
    public int getCategorieId() { return categorieId; }
    public boolean isExterne() { return externe; }
    public boolean isCoupsProfonds() { return coupsProfonds; }
    public List<String> getCoups() { return coups; }
    public int getScore() { return score; }
    public int getScoreMax() { return 10 * coups.size(); }
    public int getScoreProfond() { return scoreProfond; }
    public int getScoreProfondMax() { return 100 * coups.size(); }
    public String getCoupsTexte() { return String.join(" ", coups); }

    // La note du tir sur 100 : la moyenne des coups profonds, ou la moyenne des points × 10.
    // Un coup manqué compte 0.
    public double getNoteSur100() {
        return coupsProfonds ? (double) scoreProfond / coups.size() : 10.0 * score / coups.size();
    }

    // Pour la feuille de résultat : « 9 ↗ (87) », « Manqué ».
    public List<String> getCoupsAffiches() {
        List<String> lignes = new ArrayList<>();
        for (String coup : coups) {
            if (coup.equals(MANQUE)) {
                lignes.add("Manqué");
                continue;
            }
            Matcher m = COUP.matcher(coup);
            m.matches();
            String ligne = m.group(1);
            if (m.group(3) != null) {
                ligne += " " + FLECHES.get(m.group(3));
            }
            if (m.group(2) != null) {
                ligne += " (" + m.group(2) + ")";
            }
            lignes.add(ligne);
        }
        return lignes;
    }
}
