package com.mongodb.internal.diagnostics.logging;

import anarchy.minesky.AnarchyClans;

public final class Loggers {

    // reescrevendo a classe padrão de logging slf4j do MongoDB
    // pra evitar spam no console

    private static final String PREFIX = "org.mongodb.driver";
    private static final java.util.logging.Logger loger = AnarchyClans.l;

    public static Logger getLogger(final String suffix) {
        return new LoggerWrapper(loger);
    }

    private static class LoggerWrapper implements Logger {
        private final java.util.logging.Logger logger;

        public LoggerWrapper(java.util.logging.Logger logger) {
            this.logger = logger;
        }

        @Override
        public String getName() {
            return logger.getName();
        }


        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public void warn(String msg) {
            logger.warning(msg);
        }

        @Override
        public void warn(String msg, Throwable t) {
            logger.warning(msg);
        }

        @Override
        public boolean isErrorEnabled() {
            return true;
        }

        @Override
        public void error(String msg) {
            logger.severe(msg);
        }

        @Override
        public void error(String msg, Throwable t) {
            logger.severe(msg);
        }
    }

}