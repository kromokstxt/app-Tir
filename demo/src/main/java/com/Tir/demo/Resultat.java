package com.Tir.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Une feuille de résultat. Chaque coup a ses points (0 à 10, ou M = manqué),
// la direction de la flèche (H = haut, BD = bas-droite, …) et, en concours,
// le coup profond (0 à 100, 100 = plein centre).
//
// Un coup est gardé sous forme de texte : « 9>HD » (9, en haut à droite),
// « 9:87>HD » (avec le coup profond 87), « 10 » (sans direction) ou « M ».
public class Resultat implements Identifiable {

    public static final String MANQUE = "M";
    public static final List<String> DIRECTIONS = List.of("H", "HD", "D", "BD", "B", "BG", "G", "HG");
    public static final Map<String, String> FLECHES = Map.of(
            "H", "↑", "HD", "↗", "D", "→", "BD", "↘", "B", "↓", "BG", "↙", "G", "←", "HG", "↖");

    private static final Pattern COUP = Pattern.compile("(\\d{1,2})(?::(\\d{1,3}))?(?:>([A-Z]{1,2}))?");

    private int id;
    private String date;
    private int seanceId;
    private int categorieId;
    private boolean concours;
    private List<String> coups;
    private int score;
    private int scoreProfond;

    public Resultat(int id, String date, int seanceId, int categorieId, boolean concours, List<String> coups) {
        this.id = id;
        this.date = date;
        this.seanceId = seanceId;
        this.categorieId = categorieId;
        this.concours = concours;
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

    // Lit les coups envoyés par le formulaire, séparés par des espaces.
    // En concours, chaque coup touché doit avoir son coup profond ; à l'entraînement, il est ignoré.
    // Renvoie null si un coup n'est pas valable.
    public static List<String> lireCoups(String texte, boolean concours) {
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
            int points = Integer.parseInt(m.group(1));
            String direction = m.group(3);
            if (points > 10 || (direction != null && !DIRECTIONS.contains(direction))) {
                return null;
            }
            String propre = String.valueOf(points);
            if (concours) {
                if (m.group(2) == null || Integer.parseInt(m.group(2)) > 100) {
                    return null;
                }
                propre += ":" + Integer.parseInt(m.group(2));
            }
            if (direction != null) {
                propre += ">" + direction;
            }
            coups.add(propre);
        }
        return coups.isEmpty() ? null : coups;
    }

    public int getId() { return id; }
    public String getDate() { return date; }
    public int getSeanceId() { return seanceId; }
    public int getCategorieId() { return categorieId; }
    public boolean isConcours() { return concours; }
    public List<String> getCoups() { return coups; }
    public int getScore() { return score; }
    public int getScoreMax() { return 10 * coups.size(); }
    public int getScoreProfond() { return scoreProfond; }
    public int getScoreProfondMax() { return 100 * coups.size(); }
    public String getCoupsTexte() { return String.join(" ", coups); }

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
