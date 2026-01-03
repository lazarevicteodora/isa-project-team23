package rs.ac.uns.ftn.isa.isa_project.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import rs.ac.uns.ftn.isa.isa_project.security.RestAuthenticationEntryPoint;
import rs.ac.uns.ftn.isa.isa_project.security.TokenAuthenticationFilter;
import rs.ac.uns.ftn.isa.isa_project.service.CustomUserDetailsService;
import rs.ac.uns.ftn.isa.isa_project.util.TokenUtils;

/**
 * Glavna Spring Security konfiguracija.
 * Definiše:
 * - JWT autentifikaciju (umesto session-based)
 * - BCrypt password encoder
 * - Koja ruta zahteva autentifikaciju, a koja ne
 * - CORS i CSRF postavke
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class WebSecurityConfig {

    /**
     * Servis koji učitava korisnike iz baze
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailsService();
    }

    /**
     * BCrypt encoder za heširanje lozinki
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication provider koji koristi UserDetailsService i PasswordEncoder
     */
    /**
     * Authentication provider koji koristi UserDetailsService i PasswordEncoder
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Nova verzija zahteva UserDetailsService odmah u konstruktoru
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());

        // Zatim normalno postavljamo password encoder
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    /**
     * Handler za vraćanje 401 Unauthorized
     */
    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    /**
     * AuthenticationManager - Spring Security ga koristi za autentifikaciju
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * TokenUtils - za generisanje i validaciju JWT tokena
     */
    @Autowired
    private TokenUtils tokenUtils;

    /**
     * Glavna Security konfiguracija - definišemo pristupna pravila
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // STATELESS session - ne čuvamo sesije na serveru (koristimo JWT)
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // Sve neautentifikovane zahteve obradi uniformno - vrati 401
        http.exceptionHandling(exception ->
                exception.authenticationEntryPoint(restAuthenticationEntryPoint)
        );

        // Definišemo koja ruta zahteva autentifikaciju
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()

                // GET zahtevi za videe su javni (zadatak 3.1)
                .requestMatchers(HttpMethod.GET, "/api/videos/**").permitAll()

                // POST zahtevi zahtevaju autentifikaciju
                .requestMatchers(HttpMethod.POST, "/api/videos/**").authenticated()

                .anyRequest().authenticated()
        );

        // CORS konfiguracija - koristi CorsConfig bean
        http.cors(cors -> cors.configure(http));

        // CSRF - onemogućeno jer koristimo JWT (ne cookie-based auth)
        http.csrf(csrf -> csrf.disable());

        // Dodaj custom JWT filter PRIJE BasicAuthenticationFilter-a
        http.addFilterBefore(
                new TokenAuthenticationFilter(tokenUtils, userDetailsService()),
                BasicAuthenticationFilter.class
        );

        // Postavi authentication provider
        http.authenticationProvider(authenticationProvider());

        return http.build();
    }
}