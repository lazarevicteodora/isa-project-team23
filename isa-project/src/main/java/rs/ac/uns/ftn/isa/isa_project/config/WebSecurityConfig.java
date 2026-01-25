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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class WebSecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailsService();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());

        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Autowired
    private TokenUtils tokenUtils;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.exceptionHandling(exception ->
                exception.authenticationEntryPoint(restAuthenticationEntryPoint)
        );

        http.authorizeHttpRequests(auth -> auth
                // Auth endpoint-i - javno dostupni
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/user/*").permitAll()

                // Video GET endpoint-i - javno dostupni (ZADATAK 3.1)
                .requestMatchers(HttpMethod.GET, "/api/videos").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/videos/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/thumbnail").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/stream").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/comments").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/comments/count").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/likes/count").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/videos/*/likes/status").permitAll()
                .requestMatchers("/health").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/videos/*/view").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/videos/*/comments").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/videos/*/likes").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/videos").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/videos/*/comments/*").authenticated()

                // Sve ostalo zahteva autentifikaciju
                .anyRequest().authenticated()
        );

        http.cors(cors -> cors.configure(http));

        http.csrf(csrf -> csrf.disable());

        http.addFilterBefore(
                new TokenAuthenticationFilter(tokenUtils, userDetailsService()),
                BasicAuthenticationFilter.class
        );

        http.authenticationProvider(authenticationProvider());

        return http.build();
    }
}