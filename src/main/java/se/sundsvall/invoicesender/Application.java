package se.sundsvall.invoicesender;

import static org.springframework.boot.SpringApplication.run;

import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;

import se.sundsvall.dept44.ServiceApplication;

@ServiceApplication(exclude = ThymeleafAutoConfiguration.class)
public class Application {

	public static void main(final String... args) {
		run(Application.class, args);
	}
}
