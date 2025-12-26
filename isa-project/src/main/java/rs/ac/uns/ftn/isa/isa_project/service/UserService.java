package rs.ac.uns.ftn.isa.isa_project.service;

import rs.ac.uns.ftn.isa.isa_project.dto.RegisterRequest;
import rs.ac.uns.ftn.isa.isa_project.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Interfejs za servis koji upravlja korisnicima.
 */
public interface UserService {

    /**
     * Pronalazi korisnika po ID-u
     */
    Optional<User> findById(Long id);

    /**
     * Pronalazi korisnika po email-u
     */
    Optional<User> findByEmail(String email);

    /**
     * Pronalazi korisnika po username-u
     */
    Optional<User> findByUsername(String username);

    /**
     * Vraća sve korisnike
     */
    List<User> findAll();

    /**
     * Čuva korisnika u bazi
     */
    User save(User user);

    /**
     * Registruje novog korisnika
     * @param registerRequest podaci za registraciju
     * @return kreirani korisnik
     */
    User register(RegisterRequest registerRequest);

    /**
     * Aktivira korisnikov nalog preko aktivacionog tokena
     * @param token aktivacioni token
     * @return true ako je aktivacija uspešna
     */
    boolean activateAccount(String token);
}