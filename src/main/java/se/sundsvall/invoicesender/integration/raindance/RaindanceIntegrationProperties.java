package se.sundsvall.invoicesender.integration.raindance;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("integration.raindance")
record RaindanceIntegrationProperties(

        @NotBlank
        String host,
        @DefaultValue("445")
        int port,
        @NotBlank
        String domain,
        @NotBlank
        String username,
        @NotBlank
        String password,

        @NotBlank
        String share,

        @NotEmpty
        Map<String, BatchSetup> batchSetup,

        @DefaultValue("PT30S")
        Duration connectTimeout,
        @DefaultValue("PT30S")
        Duration responseTimeout,

        @NotBlank
        String localWorkDirectory,

        @DefaultValue("")
        String outputFileExtraSuffix) {

    record BatchSetup(@NotBlank String targetPath, String archivePath, boolean process) { }

    Properties jcifsProperties() {
        var jcifsProperties = new Properties();
        jcifsProperties.setProperty("jcifs.smb.client.connTimeout", Long.toString(connectTimeout().toMillis()));
        jcifsProperties.setProperty("jcifs.smb.client.responseTimeout", Long.toString(responseTimeout().toMillis()));
        jcifsProperties.setProperty("jcifs.smb.client.minVersion", "SMB300");
        jcifsProperties.setProperty("jcifs.smb.client.maxVersion", "SMB311");
        return jcifsProperties;
    }
}
