package se.sundsvall.invoicesender.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "invoice-processor.restart")
record ScheduledRestartProperties(

    @DefaultValue("false")
    boolean enabled,

    @DefaultValue("-")
    String cronExpression) { }
