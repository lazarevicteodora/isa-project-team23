package rs.ac.uns.ftn.isa.isa_project.service;

import rs.ac.uns.ftn.isa.isa_project.dto.LoginRequest;
import rs.ac.uns.ftn.isa.isa_project.dto.RegisterRequest;
import rs.ac.uns.ftn.isa.isa_project.dto.UserTokenState;
import rs.ac.uns.ftn.isa.isa_project.model.User;

public interface AuthService {

    User register(RegisterRequest registerRequest);

    UserTokenState login(LoginRequest loginRequest);

    boolean activateAccount(String token);
}