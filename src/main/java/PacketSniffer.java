
import config.AppConfig;
import feature.FeatureSender;
import flow.Flow;
import flow.FlowKey;
import org.pcap4j.core.*;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.*;

/**
 * Main class for the Network Traffic Analyzer application.
 * Captures network packets, processes them into flows, and sends flow features
 * to an external API for analysis.
 */
public class PacketSniffer {
    private static final Logger log = LoggerFactory.getLogger(PacketSniffer.class);

    public static void main(String[] args) {
        log.info("Starting Network Traffic Analyzer");

        // Load configuration
        AppConfig config = AppConfig.getInstance();
        String interfaceName = config.getNetworkInterface();
        int threadPoolSize = config.getThreadPoolSize();

        // Create thread pool for packet processing
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        ConcurrentHashMap<FlowKey, Flow> flows = new ConcurrentHashMap<>();

        try {
            // Find network interface
            PcapNetworkInterface nif;
            if ("any".equalsIgnoreCase(interfaceName)) {
                // Use the first available interface
                List<PcapNetworkInterface> devices = Pcaps.findAllDevs();
                if (devices.isEmpty()) {
                    log.error("No network interfaces found");
                    return;
                }
                nif = devices.getFirst();
                log.info("Using first available network interface: {}", nif.getName());
            } else {
                // Use the specified interface
                nif = Pcaps.getDevByName(interfaceName);
                if (nif == null) {
                    log.error("Network interface '{}' not found. Available interfaces:", interfaceName);
                    List<PcapNetworkInterface> devices = Pcaps.findAllDevs();
                    for (PcapNetworkInterface device : devices) {
                        log.error(" - {}: {}", device.getName(), device.getDescription());
                    }
                    return;
                }
                log.info("Using network interface: {}", nif.getName());
            }

            // Create packet capture handle
            try (PcapHandle handle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 50); ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor()) {
                handle.setFilter("ip", BpfProgram.BpfCompileMode.OPTIMIZE);

                // Create scheduled task for cleaning up inactive flows
                cleaner.scheduleAtFixedRate(() -> cleanupFlows(flows), 10, 10, TimeUnit.SECONDS);

                log.info("Starting packet capture on interface {}", nif.getName());

                // Start packet capture loop
                handle.loop(-1, (Packet packet) -> executor.execute(() -> processPacket(packet, flows)));
            }
        } catch (PcapNativeException e) {
            log.error("Error initializing packet capture: {}", e.getMessage());
        } catch (NotOpenException e) {
            log.error("Error opening packet capture handle: {}", e.getMessage());
        } catch (InterruptedException e) {
            log.error("Packet capture interrupted: {}", e.getMessage());
        } finally {
            log.info("Shutting down packet processing executor");
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    private static void processPacket(Packet packet, ConcurrentHashMap<FlowKey, Flow> flows) {
        try {
            // 1. Extract IP layer information
            if (!packet.contains(IpV4Packet.class)) {
                return; // Skip non-IPv4 packets
            }
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            IpV4Packet.IpV4Header ipHeader = ipV4Packet.getHeader();

            InetAddress srcIp = ipHeader.getSrcAddr();
            InetAddress dstIp = ipHeader.getDstAddr();
            int protocol = ipHeader.getProtocol().value();

            // 2. Extract transport layer ports if applicable
            int srcPort = 0;
            int dstPort = 0;

            if (packet.contains(TcpPacket.class)) {
                TcpPacket tcpPacket = packet.get(TcpPacket.class);
                srcPort = tcpPacket.getHeader().getSrcPort().valueAsInt();
                dstPort = tcpPacket.getHeader().getDstPort().valueAsInt();
            } else if (packet.contains(UdpPacket.class)) {
                UdpPacket udpPacket = packet.get(UdpPacket.class);
                srcPort = udpPacket.getHeader().getSrcPort().valueAsInt();
                dstPort = udpPacket.getHeader().getDstPort().valueAsInt();
            }

            // 3. Create flow key (automatically normalizes direction)
            FlowKey flowKey = new FlowKey(srcIp, dstIp, srcPort, dstPort, protocol);

            // 4. Update or create flow entry
            int finalSrcPort = srcPort;
            int finalDstPort = dstPort;
            flows.compute(flowKey, (key, existingFlow) -> {
                if (existingFlow == null) {
                    // Create new flow with original direction information
                    Flow newFlow = new Flow();
                    newFlow.initialize(srcIp, dstIp, finalSrcPort, finalDstPort, protocol);
                    newFlow.update(packet);
                    return newFlow;
                } else {
                    // Update existing flow
                    existingFlow.update(packet);
                    return existingFlow;
                }
            });

        } catch (Exception e) {
            log.error("Error processing packet: {}", e.getMessage());
        }
    }

    /**
     * Cleans up inactive flows and sends their features for analysis.
     * A flow is considered inactive if it hasn't been updated for longer than the configured timeout.
     * 
     * @param flows the map of active flows
     */
    private static void cleanupFlows(ConcurrentHashMap<FlowKey, flow.Flow> flows) {
        long currentTime = System.currentTimeMillis();
        long flowTimeout = AppConfig.getInstance().getFlowTimeout();

        flows.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue().getLastUpdated() > flowTimeout) {
                boolean sent = FeatureSender.sendFeatures(entry.getValue());
                if (!sent) {
                    log.warn("Failed to send features for flow {}", entry.getKey());
                }
                return true;
            }
            return false;
        });
    }
}
