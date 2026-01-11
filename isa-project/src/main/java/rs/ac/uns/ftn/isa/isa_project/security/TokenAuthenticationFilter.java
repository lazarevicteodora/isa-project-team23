package rs.ac.uns.ftn.isa.isa_project.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import rs.ac.uns.ftn.isa.isa_project.util.TokenUtils;

public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private TokenUtils tokenUtils;
    private UserDetailsService userDetailsService;

    protected final Log LOGGER = LogFactory.getLog(getClass());

    public TokenAuthenticationFilter(TokenUtils tokenUtils, UserDetailsService userDetailsService) {
        this.tokenUtils = tokenUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        LOGGER.debug("Zahtev ka URI: " + request.getRequestURI() + " metoda: " + request.getMethod());
        if (request.getRequestURI().contains("/api/videos") && request.getMethod().equals("POST")) {
            LOGGER.debug("=== VIDEO UPLOAD REQUEST ===");
            LOGGER.debug("Content-Type: " + request.getContentType());
            LOGGER.debug("Content-Length: " + request.getContentLength());
            LOGGER.debug("Authorization header: " + request.getHeader("Authorization"));
            LOGGER.debug("============================");
        }
        String email;

        // 1. Preuzimanje JWT tokena iz zahteva (iz Authorization header-a)
        String authToken = tokenUtils.getToken(request);
        LOGGER.debug("Zahtev ka URI: " + request.getRequestURI() + " metoda: " + request.getMethod());

        try {
            if (authToken != null) {

                // 2. Čitanje email-a iz tokena
                email = tokenUtils.getEmailFromToken(authToken);

                if (email != null) {

                    // 3. Preuzimanje korisnika iz baze na osnovu email-a
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    LOGGER.debug("Token validan, postavljena autentifikacija za: " + email);

                    // 4. Provera da li je token validan
                    LOGGER.debug("Pre validacije - email: " + email + ", userDetails: " + userDetails);
                    boolean isValid = tokenUtils.validateToken(authToken, userDetails);
                    LOGGER.debug("Validacija tokena rezultat: " + isValid);

                    if (isValid) {
                        LOGGER.debug("✅ Token JE validan za: " + email);

                        // 5. Kreiranje autentifikacije i postavljanje u Security kontekst
                        TokenBasedAuthentication authentication = new TokenBasedAuthentication(userDetails);
                        authentication.setToken(authToken);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        LOGGER.debug("❌ Token NIJE validan za: " + email);
                    }
                }
            }

        } catch (ExpiredJwtException ex) {
            LOGGER.debug("Token je istekao!");
        } catch (Exception ex) {
            LOGGER.error("Filter je bacio exception:", ex);
        }

        LOGGER.debug("Prosleđujem zahtev dalje u filter chain");

        // Prosledi zahtev dalje u sledeci filter
        chain.doFilter(request, response);
    }
}