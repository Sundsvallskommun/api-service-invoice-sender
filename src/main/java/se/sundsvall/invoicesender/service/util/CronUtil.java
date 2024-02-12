package se.sundsvall.invoicesender.service.util;

import it.burning.cron.CronExpressionDescriptor;

public final class CronUtil {

    static {
        CronExpressionDescriptor.setDefaultLocale("en");
    }

    private CronUtil() { }

    public static String parseCronExpression(final String cronExpression) {
        return CronExpressionDescriptor.getDescription(cronExpression);
    }
}
