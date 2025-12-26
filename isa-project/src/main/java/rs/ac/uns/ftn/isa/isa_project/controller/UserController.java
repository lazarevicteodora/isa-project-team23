package rs.ac.uns.ftn.isa.isa_project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.service.UserService;

import java.util.List;

/**
 * REST kontroler za upravljanje korisnicima.
 *
 * Endpointi:
 * - GET /api/users/whoami - Vraća trenutno ulogovanog korisnika
 * - GET /api/users/all    - Vraća sve korisnike (samo za ADMIN)
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * GET /api/users/whoami
     *
     * Vraća informacije o trenutno ulogovanom korisniku.
     * Zahteva autentifikaciju (JWT token).
     *
     * @return trenutno ulogovani korisnik
     */
    @GetMapping("/whoami")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Niste ulogovani!");
        }

        String email = authentication.getName(); // Email iz JWT tokena
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen!"));

        return ResponseEntity.ok(user);
    }

    /**
     * GET /api/users/all
     *
     * Vraća sve korisnike u sistemu.
     * SAMO za administratore (ROLE_ADMIN).
     *
     * @return lista svih korisnika
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }
}