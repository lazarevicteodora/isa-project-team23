package rs.ac.uns.ftn.isa.isa_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Konfiguraciona klasa za CRDT inter-replica komunikaciju.
 */
@Configuration
public class CRDTConfig {

    /**
     * RestTemplate bean za HTTP komunikaciju između replika.
     *
     * Podešavanja:
     * - connectTimeout: 5s (timeout za uspostavljanje konekcije)
     * - readTimeout: 10s (timeout za čitanje odgovora)
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 sekundi
        factory.setReadTimeout(10000);    // 10 sekundi

        return new RestTemplate(factory);
    }
}