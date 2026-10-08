package com.Tir.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AccesTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ClubDonnees donnees;

    private int idDe(String username) {
        return donnees.tireurParUsername(username).getId();
    }

    @Test
    void sansConnexionOnEstRenvoyeAuLogin() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void connexionAvecLeCompteAdmin() throws Exception {
        mvc.perform(formLogin().user("admin").password("changez-moi")).andExpect(redirectedUrl("/"));
        mvc.perform(formLogin().user("admin").password("faux")).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @WithUserDetails("tireur1")
    void unTireurNePeutPasGererLesTireurs() throws Exception {
        mvc.perform(get("/tireurs")).andExpect(status().isForbidden());
        mvc.perform(post("/tireurs/" + idDe("tireur2") + "/supprimer").with(csrf())).andExpect(status().isForbidden());
        assertThat(donnees.tireurParUsername("tireur2")).isNotNull();
    }

    @Test
    @WithUserDetails("tireur1")
    void unTireurModifieSonProfil() throws Exception {
        mvc.perform(post("/profil/modifier").with(csrf()).param("firstName", "Jean").param("lastName", "Dupont"))
                .andExpect(redirectedUrl("/profil"));
        Shooter moi = donnees.tireurParUsername("tireur1");
        assertThat(moi.getFirstName()).isEqualTo("Jean");
        assertThat(moi.isAdmin()).isFalse();
    }

    @Test
    @WithUserDetails("tireur1")
    void uneArmeAjouteeAppartientToujoursAuTireurConnecte() throws Exception {
        // Même s'il essaie de la mettre au nom de tireur2.
        mvc.perform(post("/armes/ajouter").with(csrf())
                .param("categorie", "Fas 90").param("tireurId", "" + idDe("tireur2")));
        assertThat(donnees.armes).singleElement().extracting(Arme::getTireurId).isEqualTo(idDe("tireur1"));
    }

    @Test
    @WithUserDetails("tireur2")
    void unTireurNeVoitNiNeModifieLesArmesDesAutres() throws Exception {
        donnees.armes.add(new Arme(500, "Fas 57", "03", idDe("tireur1")));

        mvc.perform(get("/armes")).andExpect(status().isOk()).andExpect(content().string(not(containsString("Fas 57/03"))));
        mvc.perform(get("/armes/500/modifier")).andExpect(status().isForbidden());
        mvc.perform(post("/armes/500/modifier").with(csrf()).param("categorie", "Fas 90"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/armes/500/supprimer").with(csrf())).andExpect(status().isForbidden());
        assertThat(donnees.armes).singleElement().extracting(Arme::getNom).isEqualTo("Fas 57/03");
    }

    @Test
    @WithUserDetails("tireur2")
    void unTireurNePeutPasAjouterUnResultatSurLaSeanceDUnAutre() throws Exception {
        donnees.saisons.add(new Saison(500, "2026", "2026-01-01", "2026-12-31"));
        donnees.categories.add(new CategorieTir(501, "Fusil 300m", 300));
        donnees.seances.add(new Seance(502, idDe("tireur1"), 500, "2026-10-01", "Entraînement", "Villarepos"));

        mvc.perform(post("/resultats/ajouter").with(csrf())
                .param("date", "2026-10-01").param("seanceId", "502").param("categorieId", "501")
                .param("coups", "10>H 9>BG"))
                .andExpect(status().isForbidden());
        assertThat(donnees.resultats).isEmpty();
    }

    @Test
    @WithUserDetails("admin")
    void lAdminVoitEtModifieLesDonneesDeTous() throws Exception {
        donnees.armes.add(new Arme(500, "Fas 57", "03", idDe("tireur1")));

        mvc.perform(get("/armes")).andExpect(content().string(containsString("Fas 57/03")));
        mvc.perform(post("/armes/500/modifier").with(csrf())
                .param("categorie", "Fas 57").param("version", "02").param("tireurId", "" + idDe("tireur1")))
                .andExpect(redirectedUrl("/armes"));
        assertThat(donnees.armes).singleElement().extracting(Arme::getNom).isEqualTo("Fas 57/02");
    }

    @Test
    @WithUserDetails("tireur1")
    void seulLAdminModifieLesDonneesDuClub() throws Exception {
        mvc.perform(get("/saisons")).andExpect(status().isOk());
        mvc.perform(post("/saisons/ajouter").with(csrf())
                .param("annee", "2026").param("dateDebut", "2026-01-01").param("dateFin", "2026-12-31"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/annonces/ajouter").with(csrf())
                .param("titre", "A").param("message", "B").param("date", "2026-10-01"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/calendrier")).andExpect(status().isOk());
        mvc.perform(post("/calendrier/ajouter").with(csrf())
                .param("date", "2026-10-10").param("titre", "Tir obligatoire"))
                .andExpect(status().isForbidden());
        assertThat(donnees.saisons).isEmpty();
        assertThat(donnees.annonces).isEmpty();
        assertThat(donnees.calendrier).isEmpty();
    }

    @Test
    @WithUserDetails("admin")
    void toutesLesPagesSAffichentPourLAdmin() throws Exception {
        remplirUnPeu();
        for (String page : PAGES) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }
        for (String page : PAGES_ADMIN) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }
    }

    @Test
    @WithUserDetails("tireur1")
    void toutesLesPagesSAffichentPourUnTireur() throws Exception {
        remplirUnPeu();
        for (String page : PAGES) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }
        for (String page : PAGES_ADMIN) {
            mvc.perform(get(page)).andExpect(status().isForbidden());
        }
    }

    @Test
    void onPeutCreerSonCompteAvecSaLicence() throws Exception {
        mvc.perform(get("/inscription")).andExpect(status().isOk())
                .andExpect(content().string(containsString("votre numéro utilisé pour le politronique au stand")));
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "Jean").param("lastName", "Dupont")
                .param("username", "jdupont").param("password", "secret").param("licence", "AB1234"))
                .andExpect(redirectedUrl("/login?inscrit"));

        Shooter nouveau = donnees.tireurParUsername("jdupont");
        assertThat(nouveau.isAdmin()).isFalse();
        assertThat(donnees.licences).singleElement().satisfies(l -> {
            assertThat(l.getNumero()).isEqualTo("AB1234");
            assertThat(l.getTireurId()).isEqualTo(nouveau.getId());
        });
        mvc.perform(formLogin().user("jdupont").password("secret")).andExpect(redirectedUrl("/"));
    }

    @Test
    void inscriptionRefuseeSiLicenceInvalideOuNomDejaPris() throws Exception {
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "A").param("lastName", "B")
                .param("username", "nouveau").param("password", "x").param("licence", "12345"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("6 caractères")));
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "A").param("lastName", "B")
                .param("username", "tireur1").param("password", "x"))
                .andExpect(content().string(containsString("déjà pris")));
        assertThat(donnees.tireurParUsername("nouveau")).isNull();
        assertThat(donnees.tireurs).hasSize(3);
    }

    @Test
    void onNePeutPasSInscrireCommeAdmin() throws Exception {
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "A").param("lastName", "B")
                .param("username", "pirate").param("password", "x").param("admin", "true"));
        assertThat(donnees.tireurParUsername("pirate").isAdmin()).isFalse();
    }

    @Test
    @WithUserDetails("tireur1")
    void seulesLesCategoriesDArmeDuClubSontAcceptees() throws Exception {
        mvc.perform(get("/armes/ajouter"))
                .andExpect(content().string(containsString("Fas 57")))
                .andExpect(content().string(containsString("Fusil de sport")));
        mvc.perform(post("/armes/ajouter").with(csrf()).param("categorie", "Pistolet"))
                .andExpect(status().isBadRequest());
        // Un Fas 57 doit être un 02 ou un 03.
        mvc.perform(post("/armes/ajouter").with(csrf()).param("categorie", "Fas 57"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/armes/ajouter").with(csrf()).param("categorie", "Fas 57").param("version", "04"))
                .andExpect(status().isBadRequest());
        assertThat(donnees.armes).isEmpty();
    }

    @Test
    @WithUserDetails("tireur1")
    void leNumeroDeLicenceFaitSixCaracteres() throws Exception {
        mvc.perform(post("/licences/ajouter").with(csrf()).param("numero", "12345"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/licences/ajouter").with(csrf()).param("numero", "123456"))
                .andExpect(redirectedUrl("/licences"));
        assertThat(donnees.licences).singleElement().extracting(Licence::getNumero).isEqualTo("123456");
    }

    @Test
    void lesCategoriesDeTirSontA300m() {
        assertThat(donnees.categories).extracting(CategorieTir::getNom).containsExactlyElementsOf(Arme.CATEGORIES);
        assertThat(donnees.categories).allMatch(c -> c.getDistance() == 300);
    }

    @Test
    @WithUserDetails("admin")
    void lAdminGereLeCalendrierEtLesAnnonces() throws Exception {
        mvc.perform(post("/calendrier/ajouter").with(csrf())
                .param("date", "2026-11-20").param("heure", "19:30").param("titre", "Assemblée générale"))
                .andExpect(redirectedUrl("/calendrier"));
        mvc.perform(post("/annonces/ajouter").with(csrf())
                .param("titre", "Stand fermé").param("message", "Travaux").param("date", "2026-10-08"))
                .andExpect(redirectedUrl("/annonces"));
        mvc.perform(get("/calendrier")).andExpect(content().string(containsString("Assemblée générale")));
        mvc.perform(get("/annonces")).andExpect(content().string(containsString("Stand fermé")));
    }

    @Test
    void onPeutAjouterSesArmesEnCreantSonCompte() throws Exception {
        mvc.perform(get("/inscription"))
                .andExpect(content().string(containsString("+ Ajouter une arme")))
                .andExpect(content().string(containsString("57/03")));
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "Jean").param("lastName", "Dupont")
                .param("username", "jdupont").param("password", "secret")
                .param("categorie", "Fas 57", "", "Mousqueton", "")
                .param("version", "03", "02", "02", "02"))
                .andExpect(redirectedUrl("/login?inscrit"));

        int id = donnees.tireurParUsername("jdupont").getId();
        assertThat(donnees.armes).extracting(Arme::getNom, Arme::getTireurId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Fas 57/03", id),
                        org.assertj.core.groups.Tuple.tuple("Mousqueton", id));
    }

    @Test
    void onPeutCreerSonCompteSansArme() throws Exception {
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "Jean").param("lastName", "Dupont")
                .param("username", "jdupont").param("password", "secret")
                .param("categorie", "", "", "", "").param("version", "02", "02", "02", "02"))
                .andExpect(redirectedUrl("/login?inscrit"));
        assertThat(donnees.armes).isEmpty();
    }

    @Test
    void auMaximumQuatreArmesALInscription() throws Exception {
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "Jean").param("lastName", "Dupont")
                .param("username", "jdupont").param("password", "secret")
                .param("categorie", "Fas 90", "Fas 90", "Fas 90", "Fas 90", "Fas 90"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Au maximum 4 armes")));
        assertThat(donnees.tireurParUsername("jdupont")).isNull();
        assertThat(donnees.armes).isEmpty();
    }

    @Test
    void unFas57DoitEtreUn02OuUn03() throws Exception {
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "Jean").param("lastName", "Dupont")
                .param("username", "jdupont").param("password", "secret")
                .param("categorie", "Fas 57").param("version", "05"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("02 ou 03")));
        assertThat(donnees.tireurParUsername("jdupont")).isNull();
    }

    @Test
    @WithUserDetails("tireur1")
    void laSaisonDUneSeanceEstTrouveeDApresSaDate() throws Exception {
        donnees.saisons.add(new Saison(500, "2025", "2025-01-01", "2025-12-31"));
        donnees.saisons.add(new Saison(501, "2026", "2026-01-01", "2026-12-31"));

        mvc.perform(post("/seances/ajouter").with(csrf())
                .param("date", "2026-03-14").param("type", "Entraînement").param("lieu", "Villarepos"))
                .andExpect(redirectedUrl("/seances"));
        mvc.perform(post("/seances/ajouter").with(csrf())
                .param("date", "2030-01-01").param("type", "Entraînement").param("lieu", "Villarepos"))
                .andExpect(redirectedUrl("/seances"));

        assertThat(donnees.seances).extracting(Seance::getSaisonId).containsExactly(501, 0);
        mvc.perform(get("/seances")).andExpect(content().string(containsString("saison <span>2026</span>")));
    }

    @Test
    @WithUserDetails("tireur1")
    void lesDatesSontAujourdhuiParDefaut() throws Exception {
        String aujourdhui = java.time.LocalDate.now().toString();
        mvc.perform(get("/seances/ajouter")).andExpect(content().string(containsString("value=\"" + aujourdhui + "\"")));
        remplirUnPeu();
        mvc.perform(get("/resultats/ajouter")).andExpect(content().string(containsString("value=\"" + aujourdhui + "\"")));
    }

    @Test
    @WithUserDetails("tireur1")
    void uneFeuilleDEntrainementGardeLesPointsEtLesFleches() throws Exception {
        donnees.saisons.add(new Saison(500, "2026", "2026-01-01", "2026-12-31"));
        donnees.seances.add(new Seance(502, idDe("tireur1"), 500, "2026-10-01", "Entraînement", "Villarepos"));
        int categorie = donnees.categories.get(0).getId();

        // À l'entraînement, un coup profond éventuel est ignoré.
        mvc.perform(post("/resultats/ajouter").with(csrf())
                .param("date", "2026-10-01").param("seanceId", "502").param("categorieId", "" + categorie)
                .param("concours", "false").param("coups", "10 9>hd m 0>B 8:77>G"))
                .andExpect(redirectedUrl("/resultats"));

        Resultat feuille = donnees.resultats.get(0);
        assertThat(feuille.isConcours()).isFalse();
        assertThat(feuille.getScore()).isEqualTo(27);
        assertThat(feuille.getScoreMax()).isEqualTo(50);
        assertThat(feuille.getCoups()).containsExactly("10", "9>HD", "M", "0>B", "8>G");
        assertThat(feuille.getCoupsAffiches()).containsExactly("10", "9 ↗", "Manqué", "0 ↓", "8 ←");
    }

    @Test
    @WithUserDetails("tireur1")
    void uneFeuilleDeConcoursGardeLesCoupsProfonds() throws Exception {
        donnees.saisons.add(new Saison(500, "2026", "2026-01-01", "2026-12-31"));
        donnees.seances.add(new Seance(502, idDe("tireur1"), 500, "2026-10-01", "Concours", "Villarepos"));
        int categorie = donnees.categories.get(0).getId();

        mvc.perform(post("/resultats/ajouter").with(csrf())
                .param("date", "2026-10-01").param("seanceId", "502").param("categorieId", "" + categorie)
                .param("concours", "true").param("coups", "10:98>H 9:87>BD M"))
                .andExpect(redirectedUrl("/resultats"));

        Resultat feuille = donnees.resultats.get(0);
        assertThat(feuille.isConcours()).isTrue();
        assertThat(feuille.getScore()).isEqualTo(19);
        assertThat(feuille.getScoreProfond()).isEqualTo(185);
        assertThat(feuille.getScoreProfondMax()).isEqualTo(300);
        assertThat(feuille.getCoupsAffiches()).containsExactly("10 ↑ (98)", "9 ↘ (87)", "Manqué");

        mvc.perform(get("/resultats/" + feuille.getId()))
                .andExpect(content().string(containsString("185 / 300")))
                .andExpect(content().string(containsString("9 ↘ (87)")));
    }

    @Test
    @WithUserDetails("tireur1")
    void unCoupMalSaisiEstRefuse() throws Exception {
        donnees.saisons.add(new Saison(500, "2026", "2026-01-01", "2026-12-31"));
        donnees.seances.add(new Seance(502, idDe("tireur1"), 500, "2026-10-01", "Entraînement", "Villarepos"));
        int categorie = donnees.categories.get(0).getId();

        String[][] essais = {
                {"false", "10 11"}, {"false", "9 X"}, {"false", ""}, {"false", "-1"}, {"false", "9>Z"},
                // En concours, chaque coup touché doit avoir son coup profond, jusqu'à 100.
                {"true", "9>H"}, {"true", "9:101>H"},
        };
        for (String[] essai : essais) {
            mvc.perform(post("/resultats/ajouter").with(csrf())
                    .param("date", "2026-10-01").param("seanceId", "502").param("categorieId", "" + categorie)
                    .param("concours", essai[0]).param("coups", essai[1]))
                    .andExpect(status().isBadRequest());
        }
        assertThat(donnees.resultats).isEmpty();
    }

    @Test
    @WithUserDetails("tireur2")
    void unTireurNeVoitPasLaCibleDUnAutre() throws Exception {
        remplirUnPeu();
        mvc.perform(get("/resultats/603")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("tireur1")
    void laCibleEstToujoursAffichee() throws Exception {
        remplirUnPeu();
        // Page du résultat : grande cible avec zoom.
        mvc.perform(get("/resultats/603"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("19 / 40")))
                .andExpect(content().string(containsString("data-coups=\"10&gt;H 9&gt;BG M 0\"")))
                .andExpect(content().string(containsString("data-zoom")))
                .andExpect(content().string(containsString("Manqué")));
        // Liste : une petite cible par résultat.
        mvc.perform(get("/resultats"))
                .andExpect(content().string(containsString("cible-mini")))
                .andExpect(content().string(containsString("data-coups=\"10&gt;H 9&gt;BG M 0\"")));
        // Formulaire : le clavier et l'aperçu de la cible.
        mvc.perform(get("/resultats/ajouter"))
                .andExpect(content().string(containsString("id=\"apercu\"")))
                .andExpect(content().string(containsString("ajouter('M')")))
                .andExpect(content().string(containsString("choisirDirection('HD')")))
                .andExpect(content().string(containsString("Concours (coups profonds)")));
        mvc.perform(get("/cible.js")).andExpect(status().isOk());
    }

    @Test
    void lesFichiersDeMiseEnPageSontPublics() throws Exception {
        mvc.perform(get("/style.css")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(content().string(containsString("width=device-width")));
    }

    private static final String[] PAGES = {
            "/", "/profil", "/profil/modifier",
            "/armes", "/armes/ajouter", "/armes/600/modifier",
            "/licences", "/licences/ajouter", "/licences/601/modifier",
            "/seances", "/seances/ajouter", "/seances/602/modifier",
            "/resultats", "/resultats/ajouter", "/resultats/603", "/resultats/603/modifier",
            "/saisons", "/categories", "/classement", "/annonces", "/calendrier",
    };

    private static final String[] PAGES_ADMIN = {
            "/tireurs", "/tireurs/ajouter", "/tireurs/2/modifier",
            "/saisons/ajouter", "/saisons/604/modifier",
            "/categories/ajouter", "/categories/605/modifier",
            "/classement/ajouter", "/classement/606/modifier",
            "/annonces/ajouter", "/annonces/607/modifier",
            "/calendrier/ajouter", "/calendrier/608/modifier",
    };

    // Des données appartenant à tireur1, pour que chaque page ait quelque chose à afficher.
    private void remplirUnPeu() {
        int t1 = idDe("tireur1");
        donnees.armes.add(new Arme(600, "Fas 90", "", t1));
        donnees.licences.add(new Licence(601, "12345", "2027-12-31", t1));
        donnees.seances.add(new Seance(602, t1, 604, "2026-10-01", "Entraînement", "Villarepos"));
        donnees.resultats.add(new Resultat(603, "2026-10-01", 602, 605, false, List.of("10>H", "9>BG", "M", "0")));
        donnees.saisons.add(new Saison(604, "2026", "2026-01-01", "2026-12-31"));
        donnees.categories.add(new CategorieTir(605, "Fusil 300m", 300));
        donnees.classements.add(new Classement(606, 1, 95, 604, t1));
        donnees.annonces.add(new Annonce(607, "Assemblée générale", "Le 20 novembre au stand.", "2026-10-01"));
        donnees.calendrier.add(new Evenement(608, "2026-11-20", "19:30", "Assemblée générale", "Au stand"));
    }
}
