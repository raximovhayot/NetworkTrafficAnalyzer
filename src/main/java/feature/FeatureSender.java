package feature;

import config.AppConfig;
import flow.Flow;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Responsible for sending flow features to the analysis API.
 * Includes retry mechanism and configurable timeout.
 */
public class FeatureSender {
    private static final Logger log = LoggerFactory.getLogger(FeatureSender.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    // Singleton HTTP client for better performance
    private static final CloseableHttpClient httpClient;

    static {
        AppConfig config = AppConfig.getInstance();
        int timeout = config.getHttpTimeout();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();

        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        // Add shutdown hook to close the HTTP client when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                httpClient.close();
                log.info("HTTP client closed");
            } catch (IOException e) {
                log.error("Error closing HTTP client: {}", e.getMessage());
            }
        }));
    }

    /**
     * Sends flow features to the analysis API with retry mechanism
     * @param flow the flow containing features to send
     * @return true if sending was successful, false otherwise
     */
    public static boolean sendFeatures(Flow flow) {
        AppConfig config = AppConfig.getInstance();
        String apiUrl = config.getApiUrl();
        int maxRetries = config.getHttpRetryCount();

        HttpPost post = new HttpPost(apiUrl);
        post.setHeader("Content-Type", "application/json");

        try {
            String jsonPayload = mapper.writeValueAsString(flow.getFeatures());
            log.info("Sending features to {}", apiUrl);
            log.info("jsonPayload: {}", jsonPayload);
            post.setEntity(new StringEntity(jsonPayload));

            // Initial attempt
            try {
                HttpResponse response = httpClient.execute(post);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    return true;
                } else {
                    log.warn("API returned status code {} on initial attempt", statusCode);
                }
            } catch (IOException e) {
                log.warn("Error sending features on initial attempt: {}", e.getMessage());
            }

            // Retry logic
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    HttpResponse response = httpClient.execute(post);
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode >= 200 && statusCode < 300) {
                        log.info("Successfully sent features after {} retries", attempt);
                        return true;
                    } else {
                        log.warn("API returned status code {} on retry attempt {}/{}", 
                                statusCode, attempt, maxRetries);

                        if (attempt == maxRetries) {
                            log.error("Failed to send features after {} retries. Last status code: {}", 
                                    maxRetries, statusCode);
                            return false;
                        }
                    }
                } catch (IOException e) {
                    if (attempt == maxRetries) {
                        log.error("Failed to send features after {} retries: {}", 
                                maxRetries, e.getMessage());
                        return false;
                    }
                    log.warn("Error sending features (retry attempt {}/{}): {}", 
                            attempt, maxRetries, e.getMessage());
                }

                // Exponential backoff
                long backoffTime = (long) (Math.pow(2, attempt - 1) * 100);
                try {
                    Thread.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Retry interrupted");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error preparing features for sending: {}", e.getMessage());
        }

        return false;
    }
}
