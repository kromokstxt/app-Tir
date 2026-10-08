package com.Tir.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/login", "/inscription", "/error", "/*.css", "/*.js").permitAll()
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(l -> l
                .logoutSuccessUrl("/login?deconnecte")
                .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // Les comptes sont les tireurs du club.
    @Bean
    public UserDetailsService userDetailsService(ClubDonnees donnees) {
        return username -> {
            Shooter tireur = donnees.tireurParUsername(username);
            if (tireur == null) {
                throw new UsernameNotFoundException(username);
            }
            return User.withUsername(tireur.getUsername())
                    .password(tireur.getPassword())
                    .roles(tireur.isAdmin() ? "ADMIN" : "TIREUR")
                    .build();
        };
    }
}
