package rs.ac.uns.ftn.isa.isa_project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rs.ac.uns.ftn.isa.isa_project.dto.RegisterRequest;
import rs.ac.uns.ftn.isa.isa_project.model.Role;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.repository.RoleRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementacija UserService interfejsa.
 * Sadrži biznis logiku za rad sa korisnicima.
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Registruje novog korisnika.
     *
     * Koraci:
     * 1. Proveri da li email već postoji
     * 2. Proveri da li username već postoji
     * 3. Proveri da li se lozinke poklapaju
     * 4. Heširaj lozinku
     * 5. Generiši aktivacioni token
     * 6. Dodeli ROLE_USER rolu
     * 7. Sačuvaj korisnika
     * 8. Pošalji aktivacioni email
     */
    @Override
    @Transactional
    public User register(RegisterRequest request) {
        // 1. Provera da li email već postoji
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email '" + request.getEmail() + "' je već zauzet!");
        }

        // 2. Provera da li username već postoji
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Korisničko ime '" + request.getUsername() + "' je već zauzeto!");
        }

        // 3. Provera da li se lozinke poklapaju
        if (!request.getPassword().equals(request.getPassword2())) {
            throw new RuntimeException("Lozinke se ne poklapaju!");
        }

        // 4. Kreiranje novog korisnika
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Heširaj lozinku!
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAddress(request.getAddress());

        // 5. Generisanje aktivacionog tokena (UUID)
        String activationToken = UUID.randomUUID().toString();
        user.setActivationToken(activationToken);
        user.setActivated(false); // Nalog nije aktiviran dok ne klikne na link
        user.setEnabled(true);    // Nalog je omogućen (nije banovan)

        // 6. Dodela ROLE_USER role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER ne postoji u bazi!"));
        user.setRoles(Collections.singletonList(userRole));

        // 7. Čuvanje korisnika u bazi
        User savedUser = userRepository.save(user);

        // 8. Slanje aktivacionog email-a (asinhrono)
        try {
            emailService.sendActivationEmail(savedUser.getEmail(), activationToken);
        } catch (Exception e) {
            System.err.println("Greška pri slanju email-a: " + e.getMessage());
            // Ne prekidamo registraciju ako email ne uspe
        }

        return savedUser;
    }

    /**
     * Aktivira korisnikov nalog preko aktivacionog tokena.
     *
     * Koraci:
     * 1. Pronađi korisnika sa datim aktivacionim tokenom
     * 2. Proveri da li je korisnik već aktiviran
     * 3. Aktiviraj korisnika
     * 4. Obriši aktivacioni token
     * 5. Sačuvaj izmene
     */
    @Override
    @Transactional
    public boolean activateAccount(String token) {
        // 1. Pronalaženje korisnika sa datim aktivacionim tokenom
        Optional<User> userOptional = userRepository.findByActivationToken(token);

        if (userOptional.isEmpty()) {
            return false; // Token ne postoji ili je nevažeći
        }

        User user = userOptional.get();

        // 2. Provera da li je korisnik već aktiviran
        if (user.isActivated()) {
            return true; // Već aktiviran - OK
        }

        // 3. Aktivacija naloga
        user.setActivated(true);
        user.setActivationToken(null); // Obrisi token nakon aktivacije

        // 4. Čuvanje izmena
        userRepository.save(user);

        return true;
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }
}