package rs.ac.uns.ftn.isa.isa_project.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import rs.ac.uns.ftn.isa.isa_project.model.User;

/**
 * Utility klasa za rad sa JSON Web Tokenima (JWT).
 */
@Component
public class TokenUtils {

    // Izdavac tokena
    @Value("${app.name:jutjubic}")
    private String APP_NAME;

    // Tajna za potpisivanje JWT (mora biti najmanje 512 bita - 64 bajta)
    @Value("${jwt.secret}")
    private String SECRET;

    // Period važenja tokena (u milisekundama) - default 30 minuta
    @Value("${jwt.expires-in:1800000}")
    private int EXPIRES_IN;

    // Naziv header-a kroz koji se prosleđuje JWT
    @Value("${jwt.header:Authorization}")
    private String AUTH_HEADER;

    private static final String AUDIENCE_WEB = "web";

    // Algoritam za potpisivanje JWT
    private SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;

    /**
     * Kreira SecretKey objekat iz SECRET string-a
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = SECRET.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ==================== Generisanje JWT tokena ====================

    /**
     * Generiše JWT token za korisnika
     * @param email Email korisnika
     * @return JWT token
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .setIssuer(APP_NAME)
                .setSubject(email)
                .setAudience(AUDIENCE_WEB)
                .setIssuedAt(new Date())
                .setExpiration(generateExpirationDate())
                .signWith(getSigningKey(), SIGNATURE_ALGORITHM)
                .compact();
    }

    /**
     * Generiše datum do kog je JWT token validan
     */
    private Date generateExpirationDate() {
        return new Date(new Date().getTime() + EXPIRES_IN);
    }

    // ==================== Čitanje informacija iz JWT tokena ====================

    /**
     * Preuzima JWT token iz HTTP zahteva
     * @param request HTTP zahtev
     * @return JWT token ili null
     */
    public String getToken(HttpServletRequest request) {
        String authHeader = getAuthHeaderFromHeader(request);

        // JWT se prosleđuje kroz header 'Authorization' u formatu:
        // Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // preuzimamo samo token (bez "Bearer " prefiksa)
        }

        return null;
    }

    /**
     * Preuzima email korisnika iz tokena
     * @param token JWT token
     * @return Email korisnika
     */
    public String getEmailFromToken(String token) {
        String email;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            email = claims.getSubject();
        } catch (ExpiredJwtException ex) {
            throw ex;
        } catch (Exception e) {
            email = null;
        }
        return email;
    }

    /**
     * Preuzima datum kreiranja tokena
     */
    public Date getIssuedAtDateFromToken(String token) {
        Date issuedAt;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            issuedAt = claims.getIssuedAt();
        } catch (ExpiredJwtException ex) {
            throw ex;
        } catch (Exception e) {
            issuedAt = null;
        }
        return issuedAt;
    }

    /**
     * Preuzima datum do kada token važi
     */
    public Date getExpirationDateFromToken(String token) {
        Date expiration;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            expiration = claims.getExpiration();
        } catch (ExpiredJwtException ex) {
            throw ex;
        } catch (Exception e) {
            expiration = null;
        }
        return expiration;
    }

    /**
     * Čita sve podatke iz JWT tokena
     */
    private Claims getAllClaimsFromToken(String token) {
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            throw ex;
        } catch (Exception e) {
            claims = null;
        }
        return claims;
    }

    // ==================== Validacija JWT tokena ====================

    /**
     * Validira JWT token
     * @param token JWT token
     * @param userDetails Informacije o korisniku
     * @return true ako je token validan
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        User user = (User) userDetails;
        final String email = getEmailFromToken(token);
        final Date created = getIssuedAtDateFromToken(token);

        // Token je validan kada:
        return (email != null
                && email.equals(user.getEmail()) // email iz tokena odgovara emailu iz baze
                && !isCreatedBeforeLastPasswordReset(created, user.getLastPasswordResetDate())); // korisnik nije menjao lozinku nakon kreiranja tokena
    }

    /**
     * Proverava da li je lozinka korisnika izmenjena nakon izdavanja tokena
     */
    private Boolean isCreatedBeforeLastPasswordReset(Date created, Date lastPasswordReset) {
        return (lastPasswordReset != null && created.before(lastPasswordReset));
    }

    // ==================== Pomocne metode ====================

    /**
     * Vraća period važenja tokena u milisekundama
     */
    public int getExpiredIn() {
        return EXPIRES_IN;
    }

    /**
     * Preuzima sadržaj Authorization header-a iz zahteva
     */
    public String getAuthHeaderFromHeader(HttpServletRequest request) {
        return request.getHeader(AUTH_HEADER);
    }
}