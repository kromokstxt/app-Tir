package com.Tir.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

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
                .param("modele", "SIG 550").param("categorie", "Fusil").param("tireurId", "" + idDe("tireur2")));
        assertThat(donnees.armes).singleElement().extracting(Arme::getTireurId).isEqualTo(idDe("tireur1"));
    }

    @Test
    @WithUserDetails("tireur2")
    void unTireurNeVoitNiNeModifieLesArmesDesAutres() throws Exception {
        donnees.armes.add(new Arme(500, "SIG 550", "Fusil", idDe("tireur1")));

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
                .param("score", "95").param("date", "2026-10-01").param("seanceId", "502").param("categorieId", "501"))
                .andExpect(status().isForbidden());
        assertThat(donnees.resultats).isEmpty();
    }

    @Test
    @WithUserDetails("admin")
    void lAdminVoitEtModifieLesDonneesDeTous() throws Exception {
        donnees.armes.add(new Arme(500, "SIG 550", "Fusil", idDe("tireur1")));

        mvc.perform(get("/armes")).andExpect(content().string(containsString("SIG 550")));
        mvc.perform(post("/armes/500/modifier").with(csrf())
                .param("modele", "SIG 551").param("categorie", "Fusil").param("tireurId", "" + idDe("tireur1")))
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
        mvc.perform(post("/comite/ajouter").with(csrf())
                .param("prenom", "A").param("nom", "B").param("fonction", "Président"))
                .andExpect(status().isForbidden());
        assertThat(donnees.saisons).isEmpty();
        assertThat(donnees.comite).isEmpty();
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

    private static final String[] PAGES = {
            "/", "/profil", "/profil/modifier",
            "/armes", "/armes/ajouter", "/armes/600/modifier",
            "/licences", "/licences/ajouter", "/licences/601/modifier",
            "/seances", "/seances/ajouter", "/seances/602/modifier",
            "/resultats", "/resultats/ajouter", "/resultats/603/modifier",
            "/saisons", "/categories", "/classement", "/comite",
    };

    private static final String[] PAGES_ADMIN = {
            "/tireurs", "/tireurs/ajouter", "/tireurs/2/modifier",
            "/saisons/ajouter", "/saisons/604/modifier",
            "/categories/ajouter", "/categories/605/modifier",
            "/classement/ajouter", "/classement/606/modifier",
            "/comite/ajouter", "/comite/607/modifier",
    };

    // Des données appartenant à tireur1, pour que chaque page ait quelque chose à afficher.
    private void remplirUnPeu() {
        int t1 = idDe("tireur1");
        donnees.armes.add(new Arme(600, "SIG 550", "Fusil", t1));
        donnees.licences.add(new Licence(601, "12345", "2027-12-31", t1));
        donnees.seances.add(new Seance(602, t1, 604, "2026-10-01", "Entraînement", "Villarepos"));
        donnees.resultats.add(new Resultat(603, 95, "2026-10-01", 602, 605));
        donnees.saisons.add(new Saison(604, "2026", "2026-01-01", "2026-12-31"));
        donnees.categories.add(new CategorieTir(605, "Fusil 300m", 300));
        donnees.classements.add(new Classement(606, 1, 95, 604, t1));
        donnees.comite.add(new MembreComite(607, "Marie", "Martin", "Présidente", 1));
    }
}
