package se.sundsvall.invoicesender.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import se.sundsvall.invoicesender.Application;

@Component
class ScheduledRestart {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledRestart.class);

    private ConfigurableApplicationContext context;
    private final boolean enabled;

    ScheduledRestart(final ConfigurableApplicationContext context,
      @Value("${scheduled-restart.enabled:false}") final boolean enabled) {
        this.context = context;
        this.enabled = enabled;

        if (enabled) {
            LOG.info("Scheduled restart is ENABLED");
        } else {
            LOG.info("Scheduled restart is DISABLED");
        }
    }

    @Scheduled(cron = "${scheduled-restart.cron-expression:-}")
    void restart() {
        if (!enabled) {
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
