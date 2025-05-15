package se.sundsvall.invoicesender.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.invoicesender.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class MessagingIntegrationPropertiesTest {

	@Autowired
	private MessagingIntegrationProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.channel()).isEqualTo("someChannel");
		assertThat(properties.connectTimeout()).isEqualTo(Duration.parse("PT1S"));
		assertThat(properties.readTimeout()).isEqualTo(Duration.parse("PT2S"));
		assertThat(properties.token()).isEqualTo("someToken");
		assertThat(properties.url()).isEqualTo("http://something.com/messaging");
		assertThat(properties.oauth2()).isNotNull().satisfies(oauth2 -> {
			assertThat(oauth2.clientId()).isEqualTo("someClientId");
			assertThat(oauth2.clientSecret()).isEqualTo("someClientSecret");
			assertThat(oauth2.tokenUrl()).isEqualTo("http://something.com/token");
		});
		assertThat(properties.invoice()).isNotNull().satisfies(invoice -> {
			assertThat(invoice.referencePrefix()).isEqualTo("Faktura #");
			assertThat(invoice.subject()).isEqualTo("someSubject");
		});
		assertThat(properties.errorReport()).isNotNull().satisfies(report -> {
			assertThat(report.recipientEmailAddresses()).hasSize(1).contains("someone.error@something.com");
			assertThat(report.senderEmailAddress()).isEqualTo("noreply.error@something.com");
			assertThat(report.senderName()).isEqualTo("SomeErrorSender");
			assertThat(report.subjectPrefix()).isEqualTo("someErrorPrefix");
		});
		assertThat(properties.statusReport()).isNotNull().satisfies(report -> {
			assertThat(report.recipientEmailAddresses()).hasSize(1).contains("someone.status@something.com");
			assertThat(report.senderEmailAddress()).isEqualTo("noreply.status@something.com");
			assertThat(report.senderName()).isEqualTo("SomeStatusSender");
			assertThat(report.subjectPrefix()).isEqualTo("someStatusPrefix");
		});
	}

}
