package rs.ac.uns.ftn.isa.isa_project.service;

import rs.ac.uns.ftn.isa.isa_project.dto.RegisterRequest;
import rs.ac.uns.ftn.isa.isa_project.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findAll();

    User save(User user);

    User register(RegisterRequest registerRequest);

    boolean activateAccount(String token);

    User getUserById(Long id);


}