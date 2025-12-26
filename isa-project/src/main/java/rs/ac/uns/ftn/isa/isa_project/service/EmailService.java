package rs.ac.uns.ftn.isa.isa_project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Servis za slanje emailova.
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Asinhrono šalje email za aktivaciju naloga
     * @param toEmail Email adresa primaoca
     * @param activationToken Token za aktivaciju
     */
    @Async
    public void sendActivationEmail(String toEmail, String activationToken) throws MailException {
        System.out.println("Slanje aktivacionog email-a na: " + toEmail);

        String activationLink = frontendUrl + "/activate/" + activationToken;
        String subject = "Jutjubić - Aktivacija naloga";
        String body = "Poštovani,\n\n" +
                "Hvala što ste se registrovali na Jutjubić platformu!\n\n" +
                "Da biste aktivirali svoj nalog, kliknite na sledeći link:\n" +
                activationLink + "\n\n" +
                "Link je validan 24 sata.\n\n" +
                "Ukoliko niste kreirali nalog, ignorišite ovaj email.\n\n" +
                "Srdačan pozdrav,\n" +
                "Jutjubić Tim";

        sendEmail(toEmail, subject, body);
        System.out.println("Aktivacioni email poslat na: " + toEmail);
    }

    /**
     * Šalje obični email
     */
    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setFrom(fromEmail);
        mail.setSubject(subject);
        mail.setText(body);
        javaMailSender.send(mail);
    }
}