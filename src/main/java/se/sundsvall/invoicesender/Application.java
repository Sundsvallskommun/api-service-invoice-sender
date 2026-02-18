package se.sundsvall.invoicesender;

import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import se.sundsvall.dept44.ServiceApplication;

import static org.springframework.boot.SpringApplication.run;

@ServiceApplication(exclude = ThymeleafAutoConfiguration.class)
public class Application {

	public static void main(final String... args) {
		run(Application.class, args);
	}
}
