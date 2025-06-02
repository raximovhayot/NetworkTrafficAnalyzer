package api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the REST API application.
 * This application provides an API for uploading parquet files containing flow features.
 */
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        log.info("Starting Network Traffic Analyzer API");

        // Create a simple HTTP server
        try {
            com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                    new java.net.InetSocketAddress(8080), 0);

            // Register the upload endpoint
            server.createContext("/api/upload", new FileUploadHandler());

            // Register the form handler for the root path
            server.createContext("/", new FileUploadFormHandler());

            // Start the server
            server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
            server.start();

            log.info("Server started on port 8080");
            log.info("Upload form available at: http://localhost:8080/");
            log.info("Upload endpoint available at: http://localhost:8080/api/upload");
        } catch (Exception e) {
            log.error("Error starting server: {}", e.getMessage(), e);
        }
    }
}
