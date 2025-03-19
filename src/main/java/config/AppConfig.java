package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration class for the Network Traffic Analyzer application.
 * Loads configuration from a properties file or uses default values.
 */
public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_FILE = "application.properties";
    private static final Properties properties = new Properties();
    private static AppConfig instance;

    // Default values
    private static final String DEFAULT_NETWORK_INTERFACE = "wlp4s0";
    private static final String DEFAULT_API_URL = "http://localhost:8080/api/flows";
    private static final int DEFAULT_THREAD_POOL_SIZE = 4;
    private static final long DEFAULT_FLOW_TIMEOUT = 60_000;
    private static final int DEFAULT_HTTP_TIMEOUT = 5000;
    private static final int DEFAULT_HTTP_RETRY_COUNT = 3;

    private AppConfig() {
        loadProperties();
    }

    /**
     * Get the singleton instance of AppConfig
     * @return AppConfig instance
     */
    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
            log.info("Loaded configuration from {}", CONFIG_FILE);
        } catch (IOException e) {
            log.warn("Could not load configuration file {}. Using default values.", CONFIG_FILE);
        }
    }

    /**
     * Get the network interface name to capture packets from
     * @return network interface name
     */
    public String getNetworkInterface() {
        return properties.getProperty("network.interface", DEFAULT_NETWORK_INTERFACE);
    }

    /**
     * Get the URL of the API to send flow features to
     * @return API URL
     */
    public String getApiUrl() {
        return properties.getProperty("api.url", DEFAULT_API_URL);
    }

    /**
     * Get the thread pool size for packet processing
     * @return thread pool size
     */
    public int getThreadPoolSize() {
        try {
            return Integer.parseInt(properties.getProperty("thread.pool.size", String.valueOf(DEFAULT_THREAD_POOL_SIZE)));
        } catch (NumberFormatException e) {
            log.warn("Invalid thread pool size in configuration. Using default: {}", DEFAULT_THREAD_POOL_SIZE);
            return DEFAULT_THREAD_POOL_SIZE;
        }
    }

    /**
     * Get the timeout for flows in milliseconds
     * @return flow timeout
     */
    public long getFlowTimeout() {
        try {
            return Long.parseLong(properties.getProperty("flow.timeout", String.valueOf(DEFAULT_FLOW_TIMEOUT)));
        } catch (NumberFormatException e) {
            log.warn("Invalid flow timeout in configuration. Using default: {}", DEFAULT_FLOW_TIMEOUT);
            return DEFAULT_FLOW_TIMEOUT;
        }
    }

    /**
     * Get the HTTP request timeout in milliseconds
     * @return HTTP timeout
     */
    public int getHttpTimeout() {
        try {
            return Integer.parseInt(properties.getProperty("http.timeout", String.valueOf(DEFAULT_HTTP_TIMEOUT)));
        } catch (NumberFormatException e) {
            log.warn("Invalid HTTP timeout in configuration. Using default: {}", DEFAULT_HTTP_TIMEOUT);
            return DEFAULT_HTTP_TIMEOUT;
        }
    }

    /**
     * Get the number of times to retry failed HTTP requests
     * @return HTTP retry count
     */
    public int getHttpRetryCount() {
        try {
            return Integer.parseInt(properties.getProperty("http.retry.count", String.valueOf(DEFAULT_HTTP_RETRY_COUNT)));
        } catch (NumberFormatException e) {
            log.warn("Invalid HTTP retry count in configuration. Using default: {}", DEFAULT_HTTP_RETRY_COUNT);
            return DEFAULT_HTTP_RETRY_COUNT;
        }
    }
}