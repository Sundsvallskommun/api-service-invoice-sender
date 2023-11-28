package se.sundsvall.invoicesender;

import static org.springframework.boot.SpringApplication.run;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import se.sundsvall.dept44.ServiceApplication;
import se.sundsvall.invoicesender.service.InvoiceProcessor;

@ServiceApplication
public class Application {

	public static void main(final String... args) {
		run(Application.class, args);
	}

	@Bean
	CommandLineRunner doStuff(final InvoiceProcessor processor) {
		return args -> {
			processor.run(LocalDate.of(2023,12,1));
		};
	}
}
