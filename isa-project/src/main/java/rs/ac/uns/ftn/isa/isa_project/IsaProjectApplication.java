package rs.ac.uns.ftn.isa.isa_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
public class IsaProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(IsaProjectApplication.class, args);
	}

}