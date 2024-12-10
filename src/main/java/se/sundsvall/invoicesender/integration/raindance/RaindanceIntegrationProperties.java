package se.sundsvall.invoicesender.integration.raindance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("integration.raindance")
public record RaindanceIntegrationProperties(Map<String, RaindanceEnvironment> environments) {

	public record RaindanceEnvironment(

		@NotBlank String host,

		@DefaultValue("445") int port,

		@NotBlank String domain,

		@NotBlank String username,

		@NotBlank String password,

		@NotBlank String share,

		List<String> invoiceFilenamePrefixes,

		@NotEmpty Map<@NotBlank String, @Valid BatchSetup> batchSetup,

		@DefaultValue("PT30S") Duration connectTimeout,
		@DefaultValue("PT30S") Duration responseTimeout,

		@NotBlank String localWorkDirectory,

		@DefaultValue("") String outputFileExtraSuffix) {

		public record BatchSetup(

			@Valid @NotNull Scheduling scheduling,

			@NotBlank String targetPath,

			String archivePath,

			boolean process) {

			public record Scheduling(@NotBlank String cronExpression) {}
		}

		Properties jcifsProperties() {
			var jcifsProperties = new Properties();
			jcifsProperties.setProperty("jcifs.smb.client.connTimeout", Long.toString(connectTimeout().toMillis()));
			jcifsProperties.setProperty("jcifs.smb.client.responseTimeout", Long.toString(responseTimeout().toMillis()));
			jcifsProperties.setProperty("jcifs.smb.client.minVersion", "SMB300");
			jcifsProperties.setProperty("jcifs.smb.client.maxVersion", "SMB311");
			return jcifsProperties;
		}
	}
}
