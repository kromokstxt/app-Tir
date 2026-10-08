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
                .param("modele", "SIG 550").param("categorie", "Fas 90").param("tireurId", "" + idDe("tireur2")));
        assertThat(donnees.armes).singleElement().extracting(Arme::getTireurId).isEqualTo(idDe("tireur1"));
    }

    @Test
    @WithUserDetails("tireur2")
    void unTireurNeVoitNiNeModifieLesArmesDesAutres() throws Exception {
        donnees.armes.add(new Arme(500, "SIG 550", "Fas 90", idDe("tireur1")));

        mvc.perform(get("/armes")).andExpect(status().isOk()).andExpect(content().string(not(containsString("SIG 550"))));
        mvc.perform(get("/armes/500/modifier")).andExpect(status().isForbidden());
        mvc.perform(post("/armes/500/modifier").with(csrf()).param("modele", "X").param("categorie", "Y"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/armes/500/supprimer").with(csrf())).andExpect(status().isForbidden());
        assertThat(donnees.armes).singleElement().extracting(Arme::getModele).isEqualTo("SIG 550");
    }

    @Test
    @WithUserDetails("tireur2")
    void unTireurNePeutPasAjouterUnResultatSurLaSeanceDUnAutre() throws Exception {
        donnees.saisons.add(new Saison(500, "2026", "2026-01-01", "2026-12-31"));
        donnees.categories.add(new CategorieTir(501, "Fusil 300m", 300));
        donnees.seances.add(new Seance(502, idDe("tireur1"), 500, "2026-10-01", "Entraînement", "Villarepos"));

        mvc.perform(post("/resultats/ajouter").with(csrf())
                .param("date", "2026-10-01").param("seanceId", "502").param("categorieId", "501")
                .param("echelle", "10").param("coups", "10 9"))
                .andExpect(status().isForbidden());
        assertThat(donnees.resultats).isEmpty();
    }

    @Test
    @WithUserDetails("admin")
    void lAdminVoitEtModifieLesDonneesDeTous() throws Exception {
        donnees.armes.add(new Arme(500, "SIG 550", "Fas 90", idDe("tireur1")));

        mvc.perform(get("/armes")).andExpect(content().string(containsString("SIG 550")));
        mvc.perform(post("/armes/500/modifier").with(csrf())
                .param("modele", "SIG 551").param("categorie", "Fas 90").param("tireurId", "" + idDe("tireur1")))
                .andExpect(redirectedUrl("/armes"));
        assertThat(donnees.armes).singleElement().extracting(Arme::getModele).isEqualTo("SIG 551");
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
        mvc.perform(post("/armes/ajouter").with(csrf()).param("modele", "X").param("categorie", "Pistolet"))
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
                .andExpect(content().string(containsString("Mousqueton")));
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "Jean").param("lastName", "Dupont")
                .param("username", "jdupont").param("password", "secret")
                .param("modele", "Fas 90 n° 1", "", "Mousqueton K31", "")
                .param("categorie", "Fas 90", "Fas 90", "Mousqueton", "Fas 90"))
                .andExpect(redirectedUrl("/login?inscrit"));

        int id = donnees.tireurParUsername("jdupont").getId();
        assertThat(donnees.armes).extracting(Arme::getModele, Arme::getCategorie, Arme::getTireurId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Fas 90 n° 1", "Fas 90", id),
                        org.assertj.core.groups.Tuple.tuple("Mousqueton K31", "Mousqueton", id));
    }

    @Test
    void onPeutCreerSonCompteSansArme() throws Exception {
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "Jean").param("lastName", "Dupont")
                .param("username", "jdupont").param("password", "secret")
                .param("modele", "", "", "", "").param("categorie", "Fas 90", "Fas 90", "Fas 90", "Fas 90"))
                .andExpect(redirectedUrl("/login?inscrit"));
        assertThat(donnees.armes).isEmpty();
    }

    @Test
    void auMaximumQuatreArmesALInscription() throws Exception {
        mvc.perform(post("/inscription").with(csrf())
                .param("firstName", "Jean").param("lastName", "Dupont")
                .param("username", "jdupont").param("password", "secret")
                .param("modele", "A", "B", "C", "D", "E")
                .param("categorie", "Fas 90", "Fas 90", "Fas 90", "Fas 90", "Fas 90"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Au maximum 4 armes")));
        assertThat(donnees.tireurParUsername("jdupont")).isNull();
        assertThat(donnees.armes).isEmpty();
    }

    @Test
    @WithUserDetails("tireur1")
    void uneFeuilleDeResultatCalculeLeTotal() throws Exception {
        donnees.saisons.add(new Saison(500, "2026", "2026-01-01", "2026-12-31"));
        donnees.seances.add(new Seance(502, idDe("tireur1"), 500, "2026-10-01", "Entraînement", "Villarepos"));
        int categorie = donnees.categories.get(0).getId();

        mvc.perform(post("/resultats/ajouter").with(csrf())
                .param("date", "2026-10-01").param("seanceId", "502").param("categorieId", "" + categorie)
                .param("echelle", "10").param("coups", "10 9, m 0 8"))
                .andExpect(redirectedUrl("/resultats"));
        mvc.perform(post("/resultats/ajouter").with(csrf())
                .param("date", "2026-10-01").param("seanceId", "502").param("categorieId", "" + categorie)
                .param("echelle", "100").param("coups", "95 87 M 100"))
                .andExpect(redirectedUrl("/resultats"));

        assertThat(donnees.resultats).extracting(Resultat::getScore, Resultat::getScoreMax)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(27, 50), org.assertj.core.groups.Tuple.tuple(282, 400));
        assertThat(donnees.resultats.get(0).getCoups()).containsExactly("10", "9", "M", "0", "8");
    }

    @Test
    @WithUserDetails("tireur1")
    void unCoupHorsEchelleEstRefuse() throws Exception {
        donnees.saisons.add(new Saison(500, "2026", "2026-01-01", "2026-12-31"));
        donnees.seances.add(new Seance(502, idDe("tireur1"), 500, "2026-10-01", "Entraînement", "Villarepos"));
        int categorie = donnees.categories.get(0).getId();

        for (String coups : new String[] {"10 11", "9 X", "", "-1"}) {
            mvc.perform(post("/resultats/ajouter").with(csrf())
                    .param("date", "2026-10-01").param("seanceId", "502").param("categorieId", "" + categorie)
                    .param("echelle", "10").param("coups", coups))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/resultats/ajouter").with(csrf())
                .param("date", "2026-10-01").param("seanceId", "502").param("categorieId", "" + categorie)
                .param("echelle", "50").param("coups", "10"))
                .andExpect(status().isBadRequest());
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
    void laCibleMontreLesCoups() throws Exception {
        remplirUnPeu();
        mvc.perform(get("/resultats/603"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("19 / 40")))
                .andExpect(content().string(containsString("Coup 1 : 10")))
                .andExpect(content().string(containsString("Coup 4 : 0")))
                .andExpect(content().string(not(containsString("Coup 3 :"))))
                .andExpect(content().string(containsString("Manqué")));
    }

    @Test
    void chaqueCoupTombeDansSonAnneau() {
        for (int valeur = 1; valeur <= 10; valeur++) {
            double r = Cible.rayon(valeur, 10);
            // L'anneau « valeur » va du rayon (10 - valeur) * 10 à (11 - valeur) * 10.
            assertThat(r).isBetween((10.0 - valeur) * 10, (11.0 - valeur) * 10);
        }
        assertThat(Cible.rayon(0, 10)).isGreaterThan(100);
        assertThat(Cible.rayon(100, 100)).isLessThan(10);
        assertThat(Cible.rayon(55, 100)).isBetween(40.0, 50.0);
        assertThat(Cible.rayon(0, 100)).isGreaterThan(100);

        Resultat feuille = new Resultat(1, "2026-10-01", 1, 1, 10, List.of("10", "M", "5"));
        assertThat(Cible.placer(feuille)).extracting(Cible.Coup::numero).containsExactly(1, 3);
        assertThat(Cible.numeros()).hasSize(36);
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
        donnees.armes.add(new Arme(600, "SIG 550", "Fas 90", t1));
        donnees.licences.add(new Licence(601, "12345", "2027-12-31", t1));
        donnees.seances.add(new Seance(602, t1, 604, "2026-10-01", "Entraînement", "Villarepos"));
        donnees.resultats.add(new Resultat(603, "2026-10-01", 602, 605, 10, List.of("10", "9", "M", "0")));
        donnees.saisons.add(new Saison(604, "2026", "2026-01-01", "2026-12-31"));
        donnees.categories.add(new CategorieTir(605, "Fusil 300m", 300));
        donnees.classements.add(new Classement(606, 1, 95, 604, t1));
        donnees.annonces.add(new Annonce(607, "Assemblée générale", "Le 20 novembre au stand.", "2026-10-01"));
        donnees.calendrier.add(new Evenement(608, "2026-11-20", "19:30", "Assemblée générale", "Au stand"));
    }
}
