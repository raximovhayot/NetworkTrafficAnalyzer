package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import feature.FeatureSender;
import flow.Flow;
import flow.FlowFeatures;
import flow.ParquetFlow;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Handler for file upload requests.
 * Supports multipart/form-data uploads of parquet files.
 */
public class FileUploadHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(FileUploadHandler.class);
    private static final String UPLOAD_DIR = "uploads";
    private static final String BOUNDARY_PREFIX = "boundary=";
    private static final int MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    public FileUploadHandler() {
        // Create upload directory if it doesn't exist
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
            log.info("Created upload directory: {}", uploadDir.getAbsolutePath());
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("POST".equals(exchange.getRequestMethod())) {
                handlePostRequest(exchange);
            } else {
                // Only POST method is supported
                exchange.sendResponseHeaders(405, 0);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write("Method Not Allowed".getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            log.error("Error handling request: {}", e.getMessage(), e);
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        // Check content type
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            sendErrorResponse(exchange, 400, "Content-Type must be multipart/form-data");
            return;
        }

        // Extract boundary
        String boundary = extractBoundary(contentType);
        if (boundary == null) {
            sendErrorResponse(exchange, 400, "Could not extract boundary from Content-Type");
            return;
        }

        // Process the multipart request
        try (InputStream is = exchange.getRequestBody()) {
            String fileName = processMultipartRequest(is, boundary);
            if (fileName != null) {
                // Use the filename returned by processMultipartRequest
                File uploadedFile = new File(UPLOAD_DIR, fileName);

                try {
                    // Process the parquet file and send flows to the second app
                    int flowCount = processParquetFile(uploadedFile);
                    sendSuccessResponse(exchange, "File uploaded successfully: " + fileName + ". Processed " + flowCount + " flows.");
                } catch (Exception e) {
                    log.error("Error processing parquet file: {}", e.getMessage(), e);
                    sendErrorResponse(exchange, 500, "File uploaded but error processing: " + e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, 400, "No file found in request");
            }
        }
    }

    private String extractBoundary(String contentType) {
        int boundaryIndex = contentType.indexOf(BOUNDARY_PREFIX);
        if (boundaryIndex != -1) {
            return "--" + contentType.substring(boundaryIndex + BOUNDARY_PREFIX.length());
        }
        return null;
    }

    private String processMultipartRequest(InputStream is, String boundary) throws IOException {
        byte[] boundaryBytes = boundary.getBytes(StandardCharsets.UTF_8);
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        boolean inHeader = true;
        String fileName = null;
        String uniqueFileName = null;
        File outputFile = null;
        FileOutputStream fileOutputStream = null;

        try {
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                if (inHeader) {
                    // Still reading headers
                    headerBytes.write(buffer, 0, bytesRead);
                    String header = headerBytes.toString(StandardCharsets.UTF_8.name());

                    // Check if we've reached the end of headers (empty line)
                    int headerEnd = header.indexOf("\r\n\r\n");
                    if (headerEnd != -1) {
                        // Extract filename from Content-Disposition header
                        fileName = extractFileName(header);
                        if (fileName == null) {
                            log.warn("No filename found in Content-Disposition header");
                            return null;
                        }

                        // Create output file with unique name
                        uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
                        outputFile = new File(UPLOAD_DIR, uniqueFileName);
                        fileName = uniqueFileName; // Return the unique filename instead of the original
                        fileOutputStream = new FileOutputStream(outputFile);

                        // Write remaining data after headers to file
                        int dataStart = headerEnd + 4; // Skip \r\n\r\n
                        if (dataStart < headerBytes.size()) {
                            byte[] data = headerBytes.toByteArray();
                            fileOutputStream.write(data, dataStart, data.length - dataStart);
                        }

                        inHeader = false;
                    }
                } else {
                    // Check if we've reached the boundary
                    if (containsBoundary(buffer, bytesRead, boundaryBytes)) {
                        // Don't write the boundary to the file
                        int boundaryPos = findBoundaryPosition(buffer, bytesRead, boundaryBytes);
                        if (boundaryPos > 0) {
                            fileOutputStream.write(buffer, 0, boundaryPos - 2); // -2 to exclude \r\n before boundary
                        }
                        break;
                    } else {
                        fileOutputStream.write(buffer, 0, bytesRead);
                    }
                }
            }

            return uniqueFileName;
        } finally {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
    }

    private String extractFileName(String header) {
        // Look for Content-Disposition header
        int contentDispositionIndex = header.indexOf("Content-Disposition: form-data;");
        if (contentDispositionIndex == -1) {
            return null;
        }

        // Extract filename
        int filenameIndex = header.indexOf("filename=\"", contentDispositionIndex);
        if (filenameIndex == -1) {
            return null;
        }

        int filenameStart = filenameIndex + 10; // "filename=\"" length
        int filenameEnd = header.indexOf("\"", filenameStart);
        if (filenameEnd == -1) {
            return null;
        }

        return header.substring(filenameStart, filenameEnd);
    }

    private boolean containsBoundary(byte[] buffer, int length, byte[] boundary) {
        if (length < boundary.length) {
            return false;
        }

        for (int i = 0; i <= length - boundary.length; i++) {
            boolean found = true;
            for (int j = 0; j < boundary.length; j++) {
                if (buffer[i + j] != boundary[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }

        return false;
    }

    private int findBoundaryPosition(byte[] buffer, int length, byte[] boundary) {
        for (int i = 0; i <= length - boundary.length; i++) {
            boolean found = true;
            for (int j = 0; j < boundary.length; j++) {
                if (buffer[i + j] != boundary[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }

        return -1;
    }

    private void sendSuccessResponse(HttpExchange exchange, String message) throws IOException {
        byte[] response = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] response = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    /**
     * Process a parquet file and send flows to the second application
     * @param parquetFile the parquet file to process
     * @return the number of flows processed
     */
    private int processParquetFile(File parquetFile) throws IOException {
        log.info("Processing parquet file: {}", parquetFile.getAbsolutePath());
        int flowCount = 0;

        try {
            // Create Hadoop configuration
            Configuration conf = new Configuration();

            // Open the parquet file using Hadoop Path
            Path hadoopPath = new Path(parquetFile.getAbsolutePath());
            ParquetFileReader reader = ParquetFileReader.open(HadoopInputFile.fromPath(hadoopPath, conf));

            // Get the schema
            MessageType schema = reader.getFooter().getFileMetaData().getSchema();
            log.info("Parquet schema: {}", schema);

            // Read the file
            PageReadStore pages;
            while ((pages = reader.readNextRowGroup()) != null) {
                long rows = pages.getRowCount();
                log.info("Reading {} rows", rows);

                // Create a record reader
                MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);
                RecordReader<Group> recordReader = columnIO.getRecordReader(
                        pages, new GroupRecordConverter(schema));

                // Process each row
                for (int i = 0; i < rows; i++) {
                    Group group = recordReader.read();

                    // Create a FlowFeatures object from the parquet data
                    FlowFeatures features = createFlowFeaturesFromGroup(group);

                    // Create a ParquetFlow with the features
                    Flow flow = new ParquetFlow(features);

                    // Send the flow to the second application
                    boolean success = FeatureSender.sendFeatures(flow);
                    if (success) {
                        flowCount++;
                    } else {
                        log.warn("Failed to send flow {}", flowCount + 1);
                    }
                }
            }

            reader.close();
            log.info("Successfully processed {} flows", flowCount);

        } catch (Exception e) {
            log.error("Error processing parquet file: {}", e.getMessage(), e);
            throw new IOException("Error processing parquet file: " + e.getMessage(), e);
        }

        return flowCount;
    }

    /**
     * Create a FlowFeatures object from a parquet Group
     * @param group the parquet Group
     * @return a FlowFeatures object
     */
    private FlowFeatures createFlowFeaturesFromGroup(Group group) {
        FlowFeatures features = new FlowFeatures();

        try {
            // Map parquet fields to FlowFeatures fields
            // The field names should match the column names in the parquet file

            // Protocol and basic flow information
            features.protocol = (byte) group.getInteger("protocol", 0);
            features.flowDuration = group.getInteger("flow_duration", 0);
            features.totalFwdPackets = group.getInteger("total_fwd_packets", 0);
            features.totalBackwardPackets = (short) group.getInteger("total_backward_packets", 0);

            // Packet length statistics
            features.fwdPacketsLengthTotal = (float) group.getDouble("fwd_packets_length_total", 0);
            features.bwdPacketsLengthTotal = (float) group.getDouble("bwd_packets_length_total", 0);
            features.fwdPacketLengthMax = (float) group.getDouble("fwd_packet_length_max", 0);
            features.fwdPacketLengthMin = (float) group.getDouble("fwd_packet_length_min", 0);
            features.fwdPacketLengthStd = (float) group.getDouble("fwd_packet_length_std", 0);
            features.bwdPacketLengthMax = (float) group.getDouble("bwd_packet_length_max", 0);
            features.bwdPacketLengthMin = (float) group.getDouble("bwd_packet_length_min", 0);

            // Flow rate statistics
            features.flowBytesPerS = group.getDouble("flow_bytes_per_s", 0);
            features.flowPacketsPerS = group.getDouble("flow_packets_per_s", 0);
            features.bwdPacketsPerS = (float) group.getDouble("bwd_packets_per_s", 0);

            // Inter-arrival time (IAT) statistics
            features.flowIATMean = (float) group.getDouble("flow_iat_mean", 0);
            features.flowIATMin = (float) group.getDouble("flow_iat_min", 0);
            features.fwdIATTotal = (float) group.getDouble("fwd_iat_total", 0);
            features.fwdIATMean = (float) group.getDouble("fwd_iat_mean", 0);
            features.fwdIATMin = (float) group.getDouble("fwd_iat_min", 0);
            features.bwdIATTotal = (float) group.getDouble("bwd_iat_total", 0);
            features.bwdIATMean = (float) group.getDouble("bwd_iat_mean", 0);
            features.bwdIATMin = (float) group.getDouble("bwd_iat_min", 0);

            // Header information
            features.fwdHeaderLength = group.getInteger("fwd_header_length", 0);
            features.bwdHeaderLength = group.getInteger("bwd_header_length", 0);

            // Packet statistics
            features.packetLengthMax = (float) group.getDouble("packet_length_max", 0);
            features.packetLengthMean = (float) group.getDouble("packet_length_mean", 0);

            // TCP flag counts
            features.synFlagCount = (byte) group.getInteger("syn_flag_count", 0);
            features.ackFlagCount = (byte) group.getInteger("ack_flag_count", 0);
            features.urgFlagCount = (byte) group.getInteger("urg_flag_count", 0);

            // Ratio statistics
            features.downUpRatio = (float) group.getDouble("down_up_ratio", 0);

            // Activity statistics
            features.activeMean = (float) group.getDouble("active_mean", 0);
            features.activeStd = (float) group.getDouble("active_std", 0);
            features.activeMax = (float) group.getDouble("active_max", 0);
            features.activeMin = (float) group.getDouble("active_min", 0);

            // Idle statistics
            features.idleMean = (float) group.getDouble("idle_mean", 0);
            features.idleStd = (float) group.getDouble("idle_std", 0);
            features.idleMax = (float) group.getDouble("idle_max", 0);

        } catch (Exception e) {
            log.warn("Error mapping parquet data to FlowFeatures: {}", e.getMessage());
            // Continue with partial data
        }

        return features;
    }
}
