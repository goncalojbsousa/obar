package com.obar.config;

import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static final String DB_HOST = getEnv("DB_HOST", "localhost");
    private static final String DB_PORT = getEnv("DB_PORT", "5432");
    private static final String DB_NAME = getEnv("DB_NAME", "obar");
    private static final String DB_URL = getEnv("DB_URL", "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME);
    private static final String DB_USER = getEnv("DB_USER", "obar_user");
    private static final String DB_PASSWORD = getEnv("DB_PASSWORD", "obar_pass");

    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            runMigrations();
            Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
            configuration.setProperty("hibernate.connection.url", DB_URL);
            configuration.setProperty("hibernate.connection.username", DB_USER);
            configuration.setProperty("hibernate.connection.password", DB_PASSWORD);
            sessionFactory = configuration.buildSessionFactory();
        }
        return sessionFactory;
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static void runMigrations() {
        Flyway flyway = Flyway.configure()
                .dataSource(DB_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}