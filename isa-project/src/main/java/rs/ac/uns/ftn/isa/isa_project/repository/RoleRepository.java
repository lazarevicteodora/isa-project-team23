package rs.ac.uns.ftn.isa.isa_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import rs.ac.uns.ftn.isa.isa_project.model.Role;

import java.util.Optional;

/**
 * Repository interfejs za Role entitet.
 * Spring Data JPA automatski kreira implementaciju.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Pronalazi rolu po imenu (npr. "ROLE_USER")
     */
    Optional<Role> findByName(String name);
}