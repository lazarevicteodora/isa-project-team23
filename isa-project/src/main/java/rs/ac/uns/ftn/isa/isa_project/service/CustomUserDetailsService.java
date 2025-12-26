package rs.ac.uns.ftn.isa.isa_project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.repository.UserRepository;

/**
 * Implementacija UserDetailsService interfejsa.
 * Spring Security koristi ovaj servis da učita korisnika iz baze.
 *
 * NAPOMENA: U ovom primeru "username" je zapravo EMAIL adresa,
 * jer se korisnici loguju sa email-om, ne sa username-om.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Učitava korisnika iz baze na osnovu email-a.
     * Ova metoda se automatski poziva od strane Spring Security-ja.
     *
     * @param email Email adresa korisnika (ne username!)
     * @return UserDetails objekat
     * @throws UsernameNotFoundException ako korisnik ne postoji
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Korisnik sa email-om '" + email + "' nije pronađen."));

        return user;
    }
}