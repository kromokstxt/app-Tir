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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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
    void onSeConnecteAvecSonPrenomEtSonNom() throws Exception {
        // Majuscules, accents, ordre du prénom et du nom : peu importe.
        for (String nom : new String[] {"Pierre Exemple", "pierre exemple", "  EXEMPLE   Pierre ", "Piérre Exemple"}) {
            mvc.perform(formLogin().user(nom).password("tireur1")).andExpect(redirectedUrl("/"));
        }
        mvc.perform(formLogin().user("Pierre Exemple").password("tireur2")).andExpect(redirectedUrl("/login?error"));
        mvc.perform(formLogin().user("Paul Exemple").password("tireur1")).andExpect(redirectedUrl("/login?error"));
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
    void unTireurNEntrePasDeTirAuNomDUnAutre() throws Exception {
        int categorie = donnees.categories.get(0).getId();
        // Même s'il essaie de le mettre au nom de tireur1, le tir est à lui.
        mvc.perform(post("/resultats/ajouter").with(csrf())
                .param("date", "2026-10-01").param("categorieId", "" + categorie)
                .param("coups", "10>H 9>BG").param("tireurId", "" + idDe("tireur1")))
                .andExpect(redirectedUrlPattern("/resultats/*"));
        assertThat(donnees.resultats).singleElement().extracting(Resultat::getTireurId).isEqualTo(idDe("tireur2"));

        // Et il ne peut ni voir ni modifier ceux d'un autre.
        donnees.resultats.add(new Resultat(700, idDe("tireur1"), 0, "2026-10-01", categorie, false, false, List.of("10")));
        mvc.perform(get("/resultats/700")).andExpect(status().isForbidden());
        mvc.perform(post("/resultats/700/modifier").with(csrf())
                .param("date", "2026-10-01").param("categorieId", "" + categorie).param("coups", "0"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/resultats/700/supprimer").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/resultats")).andExpect(content().string(org.hamcrest.Matchers.matchesPattern("(?s).*sur 1 tir\\b.*")));
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
                .andExpect(content().string(containsString("votre numéro utilisé pour le politronique au stand")))
                .andExpect(content().string(not(containsString("name=\"username\""))));
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "Jean").param("lastName", "Dupont")
                .param("password", "secret").param("licence", "AB1234"))
                .andExpect(redirectedUrl("/login?inscrit"));

        Shooter nouveau = donnees.tireurPourConnexion("Jean Dupont");
        assertThat(nouveau.isAdmin()).isFalse();
        assertThat(donnees.licences).singleElement().satisfies(l -> {
            assertThat(l.getNumero()).isEqualTo("AB1234");
            assertThat(l.getTireurId()).isEqualTo(nouveau.getId());
        });
        mvc.perform(formLogin().user("Jean Dupont").password("secret")).andExpect(redirectedUrl("/"));
    }

    @Test
    void inscriptionRefuseeSiLicenceInvalideOuNomDejaPris() throws Exception {
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "A").param("lastName", "B")
                .param("password", "x").param("licence", "12345"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("6 caractères")));
        // Deux tireurs ne peuvent pas avoir le même nom : on ne saurait plus qui se connecte.
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "pierre").param("lastName", "EXEMPLE").param("password", "x"))
                .andExpect(content().string(containsString("porte déjà ce prénom et ce nom")));
        assertThat(donnees.tireurPourConnexion("A B")).isNull();
        assertThat(donnees.tireurs).hasSize(3);
    }

    @Test
    void onNePeutPasSInscrireCommeAdmin() throws Exception {
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "A").param("lastName", "B")
                .param("password", "x").param("admin", "true"));
        assertThat(donnees.tireurPourConnexion("A B").isAdmin()).isFalse();
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
                .param("password", "secret")
                .param("categorie", "Fas 57", "", "Mousqueton", "")
                .param("version", "03", "02", "02", "02"))
                .andExpect(redirectedUrl("/login?inscrit"));

        int id = donnees.tireurPourConnexion("Jean Dupont").getId();
        assertThat(donnees.armes).extracting(Arme::getNom, Arme::getTireurId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Fas 57/03", id),
                        org.assertj.core.groups.Tuple.tuple("Mousqueton", id));
    }

    @Test
    void onPeutCreerSonCompteSansArme() throws Exception {
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "Jean").param("lastName", "Dupont")
                .param("password", "secret")
                .param("categorie", "", "", "", "").param("version", "02", "02", "02", "02"))
                .andExpect(redirectedUrl("/login?inscrit"));
        assertThat(donnees.armes).isEmpty();
    }

    @Test
    void auMaximumQuatreArmesALInscription() throws Exception {
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "Jean").param("lastName", "Dupont")
                .param("password", "secret")
                .param("categorie", "Fas 90", "Fas 90", "Fas 90", "Fas 90", "Fas 90"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Au maximum 4 armes")));
        assertThat(donnees.tireurPourConnexion("Jean Dupont")).isNull();
        assertThat(donnees.armes).isEmpty();
    }

    @Test
    void unFas57DoitEtreUn02OuUn03() throws Exception {
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "Jean").param("lastName", "Dupont")
                .param("password", "secret")
                .param("categorie", "Fas 57").param("version", "05"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("02 ou 03")));
        assertThat(donnees.tireurPourConnexion("Jean Dupont")).isNull();
    }

    @Test
    @WithUserDetails("tireur1")
    void laSaisonDUnTirEstTrouveeDApresSaDate() throws Exception {
        donnees.saisons.add(new Saison(500, "2025", "2025-01-01", "2025-12-31"));
        donnees.saisons.add(new Saison(501, "2026", "2026-01-01", "2026-12-31"));
        int categorie = donnees.categories.get(0).getId();

        for (String date : new String[] {"2026-03-14", "2030-01-01"}) {
            mvc.perform(post("/resultats/ajouter").with(csrf())
                    .param("date", date).param("categorieId", "" + categorie).param("coups", "10"))
                    .andExpect(redirectedUrlPattern("/resultats/*"));
        }
        assertThat(donnees.resultats).extracting(Resultat::getSaisonId).containsExactly(501, 0);
    }

    @Test
    @WithUserDetails("tireur1")
    void unTirExterneEstMarque() throws Exception {
        int categorie = donnees.categories.get(0).getId();
        mvc.perform(post("/resultats/ajouter").with(csrf())
                .param("date", "2026-10-01").param("categorieId", "" + categorie)
                .param("externe", "true").param("coups", "9 9"));
        assertThat(donnees.resultats).singleElement().extracting(Resultat::isExterne).isEqualTo(true);
        mvc.perform(get("/resultats")).andExpect(content().string(containsString("Tir externe")));
    }

    @Test
    @WithUserDetails("tireur1")
    void lesResultatsMontrentLaMoyenneSur100() throws Exception {
        int t1 = idDe("tireur1");
        int categorie = donnees.categories.get(0).getId();
        // 4 dix → 100 / 100 ; 5, 5, manqué, 5 → 37,5 / 100 ; coups profonds 90 et 80 → 85 / 100.
        donnees.resultats.add(new Resultat(700, t1, 0, "2026-10-01", categorie, false, false, List.of("10", "10", "10", "10")));
        donnees.resultats.add(new Resultat(701, t1, 0, "2026-10-02", categorie, false, false, List.of("5", "5", "M", "5")));
        donnees.resultats.add(new Resultat(702, t1, 0, "2026-10-03", categorie, false, true, List.of("9:90", "8:80")));
        // Le tir d'un autre ne compte pas dans ma moyenne.
        donnees.resultats.add(new Resultat(703, idDe("tireur2"), 0, "2026-10-03", categorie, false, false, List.of("0")));

        assertThat(donnees.resultats.get(1).getNoteSur100()).isEqualTo(37.5);
        assertThat(donnees.resultats.get(2).getNoteSur100()).isEqualTo(85.0);
        // (100 + 37,5 + 85) / 3 = 74,2
        mvc.perform(get("/resultats"))
                .andExpect(content().string(org.hamcrest.Matchers.matchesPattern("(?s).*Ma moyenne.*74[.,]2 / 100.*sur 3 tirs.*")));
    }

    @Test
    @WithUserDetails("tireur1")
    void lesDatesSontAujourdhuiParDefaut() throws Exception {
        String aujourdhui = java.time.LocalDate.now().toString();
        remplirUnPeu();
        mvc.perform(get("/resultats/ajouter")).andExpect(content().string(containsString("value=\"" + aujourdhui + "\"")));
    }

    @Test
    @WithUserDetails("tireur1")
    void uneFeuilleDEntrainementGardeLesPointsEtLesFleches() throws Exception {
        int categorie = donnees.categories.get(0).getId();

        // À l'entraînement, un coup profond éventuel est ignoré.
        mvc.perform(post("/resultats/ajouter").with(csrf())
                .param("date", "2026-10-01").param("categorieId", "" + categorie)
                .param("coups", "10 9>hd m 0>B 8:77>G"))
                .andExpect(redirectedUrlPattern("/resultats/*"));

        Resultat feuille = donnees.resultats.get(0);
        assertThat(feuille.isCoupsProfonds()).isFalse();
        assertThat(feuille.getScore()).isEqualTo(27);
        assertThat(feuille.getScoreMax()).isEqualTo(50);
        assertThat(feuille.getCoups()).containsExactly("10", "9>HD", "M", "0>B", "8>G");
        assertThat(feuille.getCoupsAffiches()).containsExactly("10", "9 ↗", "Manqué", "0 ↓", "8 ←");
    }

    @Test
    @WithUserDetails("tireur1")
    void avecLesCoupsProfondsLesPointsEnSontDeduits() throws Exception {
        int categorie = donnees.categories.get(0).getId();

        // Seul le coup profond compte : des points envoyés quand même sont recalculés.
        mvc.perform(post("/resultats/ajouter").with(csrf())
                .param("date", "2026-10-01").param("categorieId", "" + categorie)
                .param("coupsProfonds", "true").param("coups", ":100>H 3:87>BD M :90 :91 :5>G :0"))
                .andExpect(redirectedUrlPattern("/resultats/*"));

        Resultat feuille = donnees.resultats.get(0);
        assertThat(feuille.isCoupsProfonds()).isTrue();
        assertThat(feuille.getCoups()).containsExactly("10:100>H", "9:87>BD", "M", "9:90", "10:91", "1:5>G", "0:0");
        assertThat(feuille.getScore()).isEqualTo(39);
        assertThat(feuille.getScoreProfond()).isEqualTo(373);
        assertThat(feuille.getScoreProfondMax()).isEqualTo(700);
        assertThat(feuille.getCoupsAffiches()).startsWith("10 ↑ (100)", "9 ↘ (87)", "Manqué");

        mvc.perform(get("/resultats/" + feuille.getId()))
                .andExpect(content().string(containsString("coups profonds : 373 sur 700")))
                .andExpect(content().string(containsString("9 ↘ (87)")));
    }

    @Test
    void lesPointsSuiventLeCoupProfond() {
        assertThat(Resultat.pointsDepuisProfond(100)).isEqualTo(10);
        assertThat(Resultat.pointsDepuisProfond(91)).isEqualTo(10);
        assertThat(Resultat.pointsDepuisProfond(90)).isEqualTo(9);
        assertThat(Resultat.pointsDepuisProfond(81)).isEqualTo(9);
        assertThat(Resultat.pointsDepuisProfond(1)).isEqualTo(1);
        assertThat(Resultat.pointsDepuisProfond(0)).isEqualTo(0);
    }

    @Test
    @WithUserDetails("tireur1")
    void unCoupMalSaisiEstRefuse() throws Exception {
        int categorie = donnees.categories.get(0).getId();

        String[][] essais = {
                {"false", "10 11"}, {"false", "9 X"}, {"false", ""}, {"false", "-1"}, {"false", "9>Z"},
                // Avec les coups profonds, chaque coup touché doit avoir son coup profond, jusqu'à 100.
                {"true", "9>H"}, {"true", ":101>H"}, {"false", ":87"},
        };
        for (String[] essai : essais) {
            mvc.perform(post("/resultats/ajouter").with(csrf())
                    .param("date", "2026-10-01").param("categorieId", "" + categorie)
                    .param("coupsProfonds", essai[0]).param("coups", essai[1]))
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
                .andExpect(content().string(containsString("19 points sur 40")))
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
                .andExpect(content().string(containsString("manque()")))
                .andExpect(content().string(containsString("choisirDirection('HD')")))
                .andExpect(content().string(containsString("id=\"bouton-externe\"")))
                .andExpect(content().string(containsString("id=\"bouton-profond\"")))
                .andExpect(content().string(containsString("id=\"bouton-fleches\"")))
                // Les flèches sont cachées tant qu'on n'a pas touché le bouton.
                .andExpect(content().string(containsString("<div id=\"zone-fleches\" hidden>")));
        mvc.perform(get("/cible.js")).andExpect(status().isOk());
    }

    @Test
    void lesFichiersDeMiseEnPageSontPublics() throws Exception {
        mvc.perform(get("/style.css")).andExpect(status().isOk());
        mvc.perform(get("/icones.svg")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(content().string(containsString("width=device-width")));
        // Chaque version a sa propre adresse : un téléphone ne garde pas une vieille copie.
        mvc.perform(get("/login")).andExpect(content().string(org.hamcrest.Matchers.matchesPattern("(?s).*/style-[0-9a-f]{32}\\.css.*")));
    }

    private static final String[] PAGES = {
            "/", "/profil", "/profil/modifier",
            "/armes", "/armes/ajouter", "/armes/600/modifier",
            "/licences", "/licences/ajouter", "/licences/601/modifier",
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
        donnees.licences.add(new Licence(601, "123456", t1));
        donnees.resultats.add(new Resultat(603, t1, 604, "2026-10-01", 605, false, false, List.of("10>H", "9>BG", "M", "0")));
        donnees.saisons.add(new Saison(604, "2026", "2026-01-01", "2026-12-31"));
        donnees.categories.add(new CategorieTir(605, "Fusil 300m", 300));
        donnees.classements.add(new Classement(606, 1, 95, 604, t1));
        donnees.annonces.add(new Annonce(607, "Assemblée générale", "Le 20 novembre au stand.", "2026-10-01"));
        donnees.calendrier.add(new Evenement(608, "2026-11-20", "19:30", "Assemblée générale", "Au stand"));
    }
}
