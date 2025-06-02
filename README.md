# Network Traffic Analyzer

A Java application for capturing and analyzing network traffic to detect DDoS attacks and other network anomalies.

## Description

This application captures network packets, processes them into flows, extracts statistical features from these flows, and sends the features to an external API for analysis and classification. It can be used as part of a DDoS detection system.

Translation:
> Used to extract necessary parameters from network traffic. The extracted parameters are sent to a separate component to determine whether it is DDoS or normal traffic.

## Features

- Captures network packets using pcap4j
- Processes packets into bidirectional flows
- Extracts over 30 statistical features from each flow
- Sends flow features to an external API for analysis
- Configurable network interface, thread pool size, timeouts, etc.
- Retry mechanism for failed API requests

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

Run the application with:

```
java -jar target/NetworkTraffiicAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Note: Use the jar-with-dependencies version as it includes all required libraries.

The application will:
1. Capture packets from the configured network interface
2. Process the packets into flows
3. Extract features from completed flows
4. Send the features to the configured API endpoint

## Architecture

The application consists of several main components:

- **PacketSniffer**: The main class that captures packets and manages flows
- **Flow**: Represents a network flow and collects statistics
- **FlowFeatures**: Contains the statistical features extracted from a flow
- **FeatureSender**: Sends flow features to the external API
- **AppConfig**: Manages application configuration
