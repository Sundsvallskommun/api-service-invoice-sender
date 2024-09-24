package se.sundsvall.invoicesender.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.commons.util.ReflectionUtils.findMethod;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class ScheduledRestartTests {

    @Test
    void verifyScheduledAnnotationCronExpressionExpression() {
        var scheduledAnnotation = findMethod(ScheduledRestart.class, "restart")
            .flatMap(restartMethod -> findAnnotation(restartMethod, Scheduled.class))
            .orElseThrow(() -> new IllegalStateException("Unable to find the 'restart' method on the " + ScheduledRestart.class.getName() + " class"));

        assertThat(scheduledAnnotation.cron()).isEqualTo("${invoice-processor.restart.cron-expression:-}");
    }
}
