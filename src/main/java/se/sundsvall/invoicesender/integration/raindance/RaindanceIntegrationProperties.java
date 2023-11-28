package se.sundsvall.invoicesender.integration.raindance;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

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
        List<String> filenamePrefixes,

        @DefaultValue("PT0.05S")
        Duration connectTimeout,
        @DefaultValue("PT0.05S")
        Duration responseTimeout,

        @Valid
        @NotNull
        Local local) {

    Properties jcifsProperties() {
        var jcifsProperties = new Properties();
        jcifsProperties.setProperty("jcifs.smb.client.connTimeout", Long.toString(connectTimeout().toMillis()));
        jcifsProperties.setProperty("jcifs.smb.client.responseTimeout", Long.toString(responseTimeout().toMillis()));
        return jcifsProperties;
    }

    record Local(

        @NotBlank
        String workDirectory,

        @NotBlank
        String unzipWorkDirectory) { }
}
