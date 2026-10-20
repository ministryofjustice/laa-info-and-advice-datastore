package uk.gov.justice.laa.ia.datastore.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.sentry.logback.SentryAppender;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/** Sends structured logs only, avoiding unredacted Logback error events and breadcrumbs. */
@Component
@ConditionalOnProperty(name = "sentry.logs.enabled", havingValue = "true", matchIfMissing = true)
public class SentryLoggingConfiguration
    implements ApplicationListener<ContextRefreshedEvent>, DisposableBean {

  private SentryAppender appender;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (appender != null) {
      return;
    }
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    appender = new SentryAppender();
    appender.setName("SENTRY_STRUCTURED_LOGS");
    appender.setContext(context);
    appender.setMinimumLevel(Level.INFO);
    appender.setMinimumEventLevel(Level.OFF);
    appender.setMinimumBreadcrumbLevel(Level.OFF);
    appender.start();
    context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
  }

  @Override
  public void destroy() {
    if (appender != null) {
      LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
      context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(appender);
      appender.stop();
      appender = null;
    }
  }
}
