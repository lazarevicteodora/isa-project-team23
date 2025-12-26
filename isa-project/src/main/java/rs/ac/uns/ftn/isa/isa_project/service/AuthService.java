package rs.ac.uns.ftn.isa.isa_project.service;

import rs.ac.uns.ftn.isa.isa_project.dto.LoginRequest;
import rs.ac.uns.ftn.isa.isa_project.dto.RegisterRequest;
import rs.ac.uns.ftn.isa.isa_project.dto.UserTokenState;
import rs.ac.uns.ftn.isa.isa_project.model.User;

/**
 * Interfejs za autentifikaciju i autorizaciju.
 */
public interface AuthService {

    /**
     * Registruje novog korisnika
     */
    User register(RegisterRequest registerRequest);

    /**
     * Autentifikuje korisnika i vraća JWT token
     */
    UserTokenState login(LoginRequest loginRequest);

    /**
     * Aktivira korisnikov nalog
     */
    boolean activateAccount(String token);
}