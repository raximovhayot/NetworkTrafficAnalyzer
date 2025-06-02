# Network Traffic Analyzer

A Java application for capturing and analyzing network traffic to detect DDoS attacks and other network anomalies. Now includes an API for uploading parquet files containing flow features for testing purposes.

## Description

This application has two main components:

1. **Network Traffic Analyzer**: Captures network packets, processes them into flows, extracts statistical features from these flows, and sends the features to an external API for analysis and classification. It can be used as part of a DDoS detection system.

2. **Parquet Upload API**: Provides a web interface and REST API for uploading parquet files containing flow features for testing purposes. This is particularly useful for testing with datasets like CICDDOS2019.

Translation:
> Used to extract necessary parameters from network traffic. The extracted parameters are sent to a separate component to determine whether it is DDoS or normal traffic.

## Features

### Network Traffic Analyzer
- Captures network packets using pcap4j
- Processes packets into bidirectional flows
- Extracts over 30 statistical features from each flow
- Sends flow features to an external API for analysis
- Configurable network interface, thread pool size, timeouts, etc.
- Retry mechanism for failed API requests

### Parquet Upload API
- Web interface for uploading parquet files
- REST API endpoint for programmatic uploads
- Support for the CICDDOS2019 dataset format
- Simple file storage system for uploaded files

## Requirements

- Java 21 or higher
- Maven 3.6 or higher
- libpcap (for packet capturing)

## Installation

1. Install libpcap:
   - Ubuntu/Debian: `sudo apt-get install libpcap-dev`
   - CentOS/RHEL: `sudo yum install libpcap-devel`
   - macOS: `brew install libpcap`
   - Windows: Install [WinPcap](https://www.winpcap.org/) or [Npcap](https://nmap.org/npcap/)

2. Clone the repository:
   ```
   git clone https://github.com/yourusername/NetworkTraffiicAnalyzer.git
   cd NetworkTraffiicAnalyzer
   ```

3. Build the application:
   ```
   mvn clean package
   ```

   This will create two JAR files in the target directory:
   - `NetworkTraffiicAnalyzer-1.0-SNAPSHOT.jar`: The main JAR file (without dependencies)
   - `NetworkTraffiicAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar`: A fat JAR with all dependencies included

## Configuration

The application can be configured using the `application.properties` file:

```properties
# Network interface to capture packets from
# Use 'any' to capture from the first available interface, or specify a particular interface name
network.interface=any

# API endpoint to send flow features to
api.url=http://localhost:5000/api/network-data

# Thread pool size for packet processing
thread.pool.size=4

# Flow timeout in milliseconds
flow.timeout=60000

# HTTP request timeout in milliseconds
http.timeout=5000

# Number of times to retry failed HTTP requests
http.retry.count=3
```

## Usage

### Running the Network Traffic Analyzer

Run the packet sniffer application with:

```
java -jar target/NetworkTraffiicAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Note: Use the jar-with-dependencies version as it includes all required libraries.

The application will:
1. Capture packets from the configured network interface
2. Process the packets into flows
3. Extract features from completed flows
4. Send the features to the configured API endpoint

### Running the Parquet Upload API

Run the API server with:

```
java -cp target/NetworkTraffiicAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar api.Application
```

The server will start on port 8080 with the following endpoints:
- `http://localhost:8080/` - Web interface for uploading parquet files
- `http://localhost:8080/api/upload` - REST API endpoint for file uploads

### Using the Parquet Upload API

#### Web Interface
1. Open a web browser and navigate to `http://localhost:8080/`
2. Use the form to select and upload a parquet file
3. The file will be stored in the `uploads` directory with a unique name

#### REST API
You can also upload files programmatically using the REST API:

```
POST /api/upload
Content-Type: multipart/form-data

file=@path/to/your/file.parquet
```

Example using curl:
```
curl -X POST -F "file=@DNS-testing.parquet" http://localhost:8080/api/upload
```

#### Example Dataset
This API is designed to work with the CICDDOS2019 dataset, specifically the DNS-testing.parquet file, which can be found at:
https://www.kaggle.com/datasets/dhoogla/cicddos2019?select=DNS-testing.parquet

## Architecture

The application consists of several main components:

### Network Traffic Analyzer Components
- **PacketSniffer**: The main class that captures packets and manages flows
- **Flow**: Represents a network flow and collects statistics
- **FlowFeatures**: Contains the statistical features extracted from a flow
- **FeatureSender**: Sends flow features to the external API
- **AppConfig**: Manages application configuration

### Parquet Upload API Components
- **Application**: The main entry point for the API server
- **FileUploadHandler**: Handles file upload requests and stores uploaded files
- **FileUploadFormHandler**: Serves the HTML form for file uploads
- **uploads/**: Directory where uploaded parquet files are stored
