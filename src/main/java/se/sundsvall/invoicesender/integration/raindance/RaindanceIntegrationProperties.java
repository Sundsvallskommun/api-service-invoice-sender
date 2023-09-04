package se.sundsvall.invoicesender.integration.raindance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

        @Valid
        @NotNull
        Local local,

        @Valid
        @NotNull
        Inbound inbound,

        @Valid
        @NotNull
        Outbound outbound) {

    public record Local(

        @NotBlank
        String workDirectory,

        @NotBlank
        String unzipWorkDirectory) { }

    public record Inbound(

        @NotBlank
        String path,

        boolean deleteFilesFromShare) { }

    public record Outbound(

        @NotBlank
        String path) { }
}
