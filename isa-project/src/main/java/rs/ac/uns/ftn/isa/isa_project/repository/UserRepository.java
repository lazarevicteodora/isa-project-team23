package rs.ac.uns.ftn.isa.isa_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import rs.ac.uns.ftn.isa.isa_project.model.User;

import java.util.Optional;

/**
 * Repository interfejs za User entitet.
 * Spring Data JPA automatski kreira implementaciju.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Pronalazi korisnika po email adresi
     */
    Optional<User> findByEmail(String email);

    /**
     * Pronalazi korisnika po korisničkom imenu
     */
    Optional<User> findByUsername(String username);

    /**
     * Pronalazi korisnika po activation token-u
     */
    Optional<User> findByActivationToken(String activationToken);

    /**
     * Proverava da li korisnik sa datim email-om postoji
     */
    boolean existsByEmail(String email);

    /**
     * Proverava da li korisnik sa datim username-om postoji
     */
    boolean existsByUsername(String username);
}