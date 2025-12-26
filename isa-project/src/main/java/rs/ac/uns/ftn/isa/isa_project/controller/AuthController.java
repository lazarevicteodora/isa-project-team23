package rs.ac.uns.ftn.isa.isa_project.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import rs.ac.uns.ftn.isa.isa_project.dto.LoginRequest;
import rs.ac.uns.ftn.isa.isa_project.dto.RegisterRequest;
import rs.ac.uns.ftn.isa.isa_project.dto.UserTokenState;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.service.AuthService;

/**
 * REST kontroler za autentifikaciju i registraciju.
 *
 * Endpointi:
 * - POST /api/auth/register - Registracija novog korisnika
 * - POST /api/auth/login    - Prijava na sistem (vraća JWT token)
 * - GET  /api/auth/activate/{token} - Aktivacija naloga preko email linka
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * POST /api/auth/register
     *
     * Registruje novog korisnika.
     * Nakon registracije, korisniku se šalje aktivacioni link na email.
     *
     * @param registerRequest podaci za registraciju (email, username, password, itd.)
     * @return kreiran korisnik (201 CREATED)
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User user = authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    "Registracija uspešna! Proverite email za aktivacioni link."
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * POST /api/auth/login
     *
     * Autentifikuje korisnika i vraća JWT token.
     * Korisnik se loguje sa email adresom i lozinkom.
     *
     * @param loginRequest email i lozinka
     * @return JWT token i vreme važenja (200 OK)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            UserTokenState token = authService.login(loginRequest);
            return ResponseEntity.ok(token);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * GET /api/auth/activate/{token}
     *
     * Aktivira korisnikov nalog preko tokena iz email-a.
     * Ovaj endpoint se poziva kada korisnik klikne na link u email-u.
     *
     * @param token aktivacioni token (UUID)
     * @return poruka o uspešnosti aktivacije
     */
    @GetMapping("/activate/{token}")
    public ResponseEntity<?> activateAccount(@PathVariable String token) {
        boolean activated = authService.activateAccount(token);

        if (activated) {
            return ResponseEntity.ok("Nalog je uspešno aktiviran! Možete se prijaviti.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    "Nevažeći ili istekao aktivacioni link!"
            );
        }
    }
}