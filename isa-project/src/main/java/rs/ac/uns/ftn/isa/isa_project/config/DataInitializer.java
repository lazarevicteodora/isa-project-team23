package rs.ac.uns.ftn.isa.isa_project.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.isa.isa_project.model.Role;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.repository.RoleRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.UserRepository;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail("admin@jutjubic.com").isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

            User admin = new User();
            admin.setEmail("admin@jutjubic.com");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Admin");
            admin.setLastName("Jutjubic");
            admin.setAddress("Admin Street 1");
            admin.setActivated(true);
            admin.setEnabled(true);
            admin.setRoles(List.of(adminRole));

            userRepository.save(admin);
            System.out.println("✅ Admin korisnik kreiran: admin@jutjubic.com / admin123");
        }
    }
}