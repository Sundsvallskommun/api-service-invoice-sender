package se.sundsvall.invoicesender.integration.raindance;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import jakarta.validation.constraints.NotBlank;

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

        List<String> batchFilenamePrefixes,

        @DefaultValue("PT30S")
        Duration connectTimeout,
        @DefaultValue("PT30S")
        Duration responseTimeout,

        @NotBlank
        String workDirectory,

        @DefaultValue("")
        String outputFileExtraSuffix) {

    Properties jcifsProperties() {
        var jcifsProperties = new Properties();
        jcifsProperties.setProperty("jcifs.smb.client.connTimeout", Long.toString(connectTimeout().toMillis()));
        jcifsProperties.setProperty("jcifs.smb.client.responseTimeout", Long.toString(responseTimeout().toMillis()));
        jcifsProperties.setProperty("jcifs.smb.client.minVersion", "SMB300");
        jcifsProperties.setProperty("jcifs.smb.client.maxVersion", "SMB311");
        return jcifsProperties;
    }
}
