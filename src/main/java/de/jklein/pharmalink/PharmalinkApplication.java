package de.jklein.pharmalink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class PharmalinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(PharmalinkApplication.class, args);
	}

}