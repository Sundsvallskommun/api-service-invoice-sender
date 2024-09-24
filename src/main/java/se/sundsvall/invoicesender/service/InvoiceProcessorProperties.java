package se.sundsvall.invoicesender.service;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "invoice-processor")
public record InvoiceProcessorProperties(

    @Valid
    @NotNull
    Schedule schedule,

    @Valid
    @NotNull
    Restart restart,

    @NotEmpty
    List<String> invoiceFilenamePrefixes) {

    public record Schedule(

        @NotBlank
        String cronExpression,

        @NotEmpty
        List<String> municipalityIds) { }

    public record Restart(

        @DefaultValue("-")
        String cronExpression,

        @DefaultValue("false")
        boolean enabled) { }
}
