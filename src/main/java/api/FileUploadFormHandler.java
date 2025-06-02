package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handler for serving the file upload form.
 */
public class FileUploadFormHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(FileUploadFormHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                String html = getUploadFormHtml();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, html.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(html.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                // Only GET method is supported
                exchange.sendResponseHeaders(405, 0);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write("Method Not Allowed".getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            log.error("Error handling request: {}", e.getMessage(), e);
            String errorMessage = "Internal Server Error: " + e.getMessage();
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(500, errorMessage.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorMessage.getBytes(StandardCharsets.UTF_8));
            }
        } finally {
            exchange.close();
        }
    }

    private String getUploadFormHtml() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Upload Parquet File</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            max-width: 800px;\n" +
                "            margin: 0 auto;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        h1 {\n" +
                "            color: #333;\n" +
                "        }\n" +
                "        .upload-form {\n" +
                "            border: 1px solid #ddd;\n" +
                "            padding: 20px;\n" +
                "            border-radius: 5px;\n" +
                "            background-color: #f9f9f9;\n" +
                "        }\n" +
                "        .form-group {\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "        label {\n" +
                "            display: block;\n" +
                "            margin-bottom: 5px;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .btn {\n" +
                "            background-color: #4CAF50;\n" +
                "            color: white;\n" +
                "            padding: 10px 15px;\n" +
                "            border: none;\n" +
                "            border-radius: 4px;\n" +
                "            cursor: pointer;\n" +
                "        }\n" +
                "        .btn:hover {\n" +
                "            background-color: #45a049;\n" +
                "        }\n" +
                "        .note {\n" +
                "            margin-top: 20px;\n" +
                "            padding: 10px;\n" +
                "            background-color: #e7f3fe;\n" +
                "            border-left: 6px solid #2196F3;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Upload Parquet File</h1>\n" +
                "    <div class=\"upload-form\">\n" +
                "        <form action=\"/api/upload\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "            <div class=\"form-group\">\n" +
                "                <label for=\"file\">Select Parquet File:</label>\n" +
                "                <input type=\"file\" id=\"file\" name=\"file\" accept=\".parquet\" required>\n" +
                "            </div>\n" +
                "            <div class=\"form-group\">\n" +
                "                <button type=\"submit\" class=\"btn\">Upload</button>\n" +
                "            </div>\n" +
                "        </form>\n" +
                "    </div>\n" +
                "    <div class=\"note\">\n" +
                "        <p><strong>Note:</strong> This form allows you to upload parquet files containing flow features for testing purposes.</p>\n" +
                "        <p>Example dataset: <a href=\"https://www.kaggle.com/datasets/dhoogla/cicddos2019?select=DNS-testing.parquet\" target=\"_blank\">CICDDOS2019 DNS-testing.parquet</a></p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}