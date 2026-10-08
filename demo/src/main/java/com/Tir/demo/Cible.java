package com.Tir.demo;

import java.util.ArrayList;
import java.util.List;

// Place les coups d'une feuille de résultat sur la cible (anneaux 1 à 10).
// La cible fait 100 de rayon : l'anneau 1 va jusqu'à 100, l'anneau 10 jusqu'à 10.
// On ne connaît que la valeur du coup, pas sa position exacte : chaque coup est placé
// dans son anneau, et les coups sont répartis tout autour pour ne pas se cacher.
public class Cible {

    public record Coup(int numero, String valeur, double x, double y) {}

    public record Numero(int anneau, double x, double y, boolean surLeNoir) {}

    private static final double ANGLE_OR = Math.toRadians(137.508);

    public static List<Coup> placer(Resultat resultat) {
        List<Coup> places = new ArrayList<>();
        List<String> coups = resultat.getCoups();
        for (int i = 0; i < coups.size(); i++) {
            String valeur = coups.get(i);
            if (valeur.equals(Resultat.MANQUE)) {
                continue;
            }
            double r = rayon(Integer.parseInt(valeur), resultat.getEchelle());
            double angle = i * ANGLE_OR - Math.PI / 2;
            places.add(new Coup(i + 1, valeur, arrondi(r * Math.cos(angle)), arrondi(r * Math.sin(angle))));
        }
        return places;
    }

    // Les numéros 1 à 9 imprimés sur les quatre diagonales, comme sur la vraie cible.
    public static List<Numero> numeros() {
        List<Numero> numeros = new ArrayList<>();
        for (int anneau = 1; anneau <= 9; anneau++) {
            double r = (10.5 - anneau) * 10;
            for (int degres = 45; degres < 360; degres += 90) {
                double angle = Math.toRadians(degres);
                numeros.add(new Numero(anneau, arrondi(r * Math.cos(angle)), arrondi(r * Math.sin(angle)), anneau >= 5));
            }
        }
        return numeros;
    }

    // Distance au centre pour une valeur : 10 (ou 100) au centre, 0 juste en dehors de l'anneau 1.
    static double rayon(int valeur, int echelle) {
        if (valeur == 0) {
            return 105;
        }
        if (echelle == 10) {
            return (10 - valeur + 0.5) * 10;
        }
        return Math.max(1, 100 - valeur + 0.5);
    }

    private static double arrondi(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
