package rs.ac.uns.ftn.isa.isa_project.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Ova klasa se poziva kada korisnik pokuša da pristupi zaštićenom resursu
 * bez validnih kredencijala (bez JWT tokena ili sa nevažećim tokenom).
 * Vraća 401 Unauthorized response.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Vraćamo 401 Unauthorized
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
    }
}