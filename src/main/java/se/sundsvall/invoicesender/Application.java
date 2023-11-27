package se.sundsvall.invoicesender;

import static org.springframework.boot.SpringApplication.run;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import se.sundsvall.dept44.ServiceApplication;
import se.sundsvall.invoicesender.integration.raindance.RaindanceIntegration;
import se.sundsvall.invoicesender.service.InvoiceSenderService;

@ServiceApplication
public class Application {

	public static void main(final String... args) {
		run(Application.class, args);
	}

	//@Bean
	CommandLineRunner doStuff2(final InvoiceSenderService service) {
		return args -> {
			service.doStuff();
		};
	}

	@Bean
	CommandLineRunner doStuff(final RaindanceIntegration raindanceIntegration) {
		return args -> {
			var batches = raindanceIntegration.readBatch();

			raindanceIntegration.writeBatch(batches.get(0));
		};
	}
}
