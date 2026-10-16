package com.Tir.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Ce qui est enregistré se retrouve au redémarrage : on relit la même base avec un nouveau ClubDonnees.
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BaseDeDonneesTests {

    @Autowired ClubDonnees donnees;
    @Autowired JdbcTemplate jdbc;
    @Autowired Club club;
    @Autowired PasswordEncoder encoder;

    private ClubDonnees redemarrer() {
        return new ClubDonnees(jdbc, club, encoder, "autre-mot-de-passe", true);
    }

    @Test
    void toutEstRetrouveApresUnRedemarrage() {
        int pierre = donnees.tireurParUsername("tireur1").getId();
        int arme = donnees.nouvelId();
        donnees.armes.add(new Arme(arme, "Fas 57", "03", pierre));
        donnees.licences.add(new Licence(donnees.nouvelId(), "AB1234", pierre));
        int tir = donnees.nouvelId();
        donnees.resultats.add(new Resultat(tir, pierre, 0, "2026-10-16", "Fas 57/03", true, true,
                List.of("9:87>HD", "10:96", "M")));
        donnees.annonces.add(new Annonce(donnees.nouvelId(), "Tir fédéral", "Samedi à 9 h", "2026-10-16"));
        ClubDonnees.remplacer(donnees.armes, new Arme(arme, "Fas 57", "02", pierre));
        int marie = donnees.tireurParUsername("tireur2").getId();
        donnees.supprimerTireur(marie);

        ClubDonnees relue = redemarrer();

        // Rien n'est créé deux fois, et le mot de passe admin du premier démarrage est gardé.
        assertThat(relue.tireurs).hasSize(2);
        assertThat(relue.categories).hasSize(4);
        assertThat(encoder.matches("changez-moi", relue.tireurParUsername("admin").getPassword())).isTrue();
        assertThat(relue.nomTireur(marie)).isEqualTo("?");

        assertThat(ClubDonnees.trouver(relue.armes, arme).getNom()).isEqualTo("Fas 57/02");
        assertThat(relue.licences).extracting(Licence::getNumero).containsExactly("AB1234");
        Resultat r = ClubDonnees.trouver(relue.resultats, tir);
        assertThat(r.getCoups()).containsExactly("9:87>HD", "10:96", "M");
        assertThat(r.isExterne()).isTrue();
        assertThat(r.isCoupsProfonds()).isTrue();
        assertThat(r.getScore()).isEqualTo(19);
        assertThat(relue.annonces).extracting(Annonce::getMessage).containsExactly("Samedi à 9 h");

        // Les nouveaux numéros continuent après ceux déjà utilisés.
        assertThat(relue.nouvelId()).isGreaterThan(tir);
    }

    @Test
    void lesAutresChangementsSontRefuses() {
        // Ils ne seraient pas enregistrés : mieux vaut une erreur tout de suite qu'une donnée perdue.
        assertThatThrownBy(() -> donnees.armes.clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
