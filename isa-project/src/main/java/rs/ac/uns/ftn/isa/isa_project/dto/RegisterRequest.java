package rs.ac.uns.ftn.isa.isa_project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email mora biti validan")
    private String email;

    @NotBlank(message = "Korisničko ime je obavezno")
    @Size(min = 3, max = 64, message = "Korisničko ime mora imati između 3 i 64 karaktera")
    private String username;

    @NotBlank(message = "Lozinka je obavezna")
    @Size(min = 6, max = 32, message = "Lozinka mora imati između 6 i 32 karaktera")
    private String password;

    @NotBlank(message = "Potvrda lozinke je obavezna")
    private String password2;

    @NotBlank(message = "Ime je obavezno")
    private String firstName;

    @NotBlank(message = "Prezime je obavezno")
    private String lastName;

    @NotBlank(message = "Adresa je obavezna")
    private String address;

    public RegisterRequest() {
    }

    public RegisterRequest(String email, String username, String password, String password2,
                           String firstName, String lastName, String address) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.password2 = password2;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}