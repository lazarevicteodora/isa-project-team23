package rs.ac.uns.ftn.isa.isa_project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import rs.ac.uns.ftn.isa.isa_project.dto.LoginRequest;
import rs.ac.uns.ftn.isa.isa_project.dto.RegisterRequest;
import rs.ac.uns.ftn.isa.isa_project.dto.UserTokenState;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.util.TokenUtils;

/**
 * Implementacija AuthService interfejsa.
 * Sadrži biznis logiku za autentifikaciju i autorizaciju.
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenUtils tokenUtils;

    /**
     * Registruje novog korisnika.
     * Poziva UserService.register() koji radi svu logiku.
     */
    @Override
    public User register(RegisterRequest registerRequest) {
        return userService.register(registerRequest);
    }

    /**
     * Autentifikuje korisnika i vraća JWT token.
     *
     * Koraci:
     * 1. Pronađi korisnika po email-u
     * 2. Proveri da li je nalog aktiviran
     * 3. Proveri lozinku (Spring Security to radi automatski)
     * 4. Generiši JWT token
     * 5. Vrati token klijentu
     */
    @Override
    public UserTokenState login(LoginRequest loginRequest) {
        // 1. Pronalaženje korisnika po email-u
        User user = userService.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Pogrešan email ili lozinka!"));

        // 2. Provera da li je nalog aktiviran
        if (!user.isActivated()) {
            throw new RuntimeException("Nalog nije aktiviran! Proverite email za aktivacioni link.");
        }

        // 3. Autentifikacija korisnika (Spring Security proverava lozinku)
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),    // username je zapravo email
                            loginRequest.getPassword()
                    )
            );

            // Postavljanje autentifikacije u Security kontekst
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            throw new RuntimeException("Pogrešan email ili lozinka!");
        }

        // 4. Generisanje JWT tokena
        String jwt = tokenUtils.generateToken(user.getEmail());
        int expiresIn = tokenUtils.getExpiredIn();

        // 5. Vraćanje tokena klijentu
        return new UserTokenState(jwt, expiresIn);
    }

    /**
     * Aktivira korisnikov nalog preko aktivacionog tokena.
     */
    @Override
    public boolean activateAccount(String token) {
        return userService.activateAccount(token);
    }
}