package se.sundsvall.invoicesender;

import static org.springframework.boot.SpringApplication.run;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.context.annotation.Bean;

import se.sundsvall.dept44.ServiceApplication;
import se.sundsvall.invoicesender.integration.messaging.MessagingIntegration;
import se.sundsvall.invoicesender.service.InvoiceProcessor;

@ServiceApplication(exclude = ThymeleafAutoConfiguration.class)
public class Application {

	public static void main(final String... args) {
		run(Application.class, args);
	}

	@Bean
	CommandLineRunner doStuff(final InvoiceProcessor processor, final MessagingIntegration messagingIntegration) {
		return args -> {
			//processor.run(LocalDate.of(2023,12,1));
			//messagingIntegration.sendStatusReport("Bla bla bla");

			/*
			var dtos = List.of(
				new BatchDto(1, "Faktura-pdf-170220_161231", LocalDateTime.of(2023, 12, 14, 8, 31), LocalDateTime.of(2023, 12, 14, 8, 32), 14, 13),
				new BatchDto(2, "Faktura-pdf-231201_021450", LocalDateTime.of(2023, 12, 15, 9, 52), LocalDateTime.of(2023, 12, 15, 9, 53), 21, 18));

			System.err.println(messagingIntegration.generateStatusReportMessage(List.of()));
			*/
		};
	}
}
