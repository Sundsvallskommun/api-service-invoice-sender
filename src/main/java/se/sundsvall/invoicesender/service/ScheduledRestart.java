package se.sundsvall.invoicesender.service;

import static se.sundsvall.invoicesender.service.util.CronUtil.parseCronExpression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import se.sundsvall.invoicesender.Application;

@Component
class ScheduledRestart {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledRestart.class);

    private final InvoiceProcessorProperties properties;
    private ConfigurableApplicationContext context;

    ScheduledRestart(final ConfigurableApplicationContext context,
            final InvoiceProcessorProperties properties) {
        this.context = context;
        this.properties = properties;

        var cronExpression = properties.restart().cronExpression();
        if (properties.restart().enabled() && !"-".equals(cronExpression)) {
            LOG.info("Scheduled restart is ENABLED to run {}", parseCronExpression(cronExpression));
        } else {
            LOG.info("Scheduled restart is DISABLED");
        }
    }

    @Scheduled(cron = "${invoice-procssor.restart.cron-expression:-}")
    void restart() {
        if (!properties.restart().enabled()) {
            return;
        }

        LOG.info("Restarting application");

        var args = context.getBean(ApplicationArguments.class);

        var thread = new Thread(() -> {
            context.close();
            context = SpringApplication.run(Application.class, args.getSourceArgs());
        });
        thread.setDaemon(false);
        thread.start();
    }
}
