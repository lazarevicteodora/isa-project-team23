package rs.ac.uns.ftn.isa.isa_project.controller;

import jakarta.servlet.http.HttpServletRequest;
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
import rs.ac.uns.ftn.isa.isa_project.service.RateLimitService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RateLimitService rateLimitService;

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

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        String ipAddress = getClientIpAddress(request);

        if (!rateLimitService.allowRequest(ipAddress)) {
            int remainingAttempts = rateLimitService.getRemainingAttempts(ipAddress);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    "Previše pokušaja prijave! Pokušajte ponovo za 1 minut. " +
                            "Preostalo pokušaja: " + remainingAttempts
            );
        }

        try {
            UserTokenState token = authService.login(loginRequest);

            rateLimitService.resetAttempts(ipAddress);

            return ResponseEntity.ok(token);
        } catch (RuntimeException e) {
            int remainingAttempts = rateLimitService.getRemainingAttempts(ipAddress);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    e.getMessage() + " Preostalo pokušaja: " + remainingAttempts
            );
        }
    }

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

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
