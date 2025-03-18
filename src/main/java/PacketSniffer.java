
import feature.FeatureSender;
import flow.Flow;
import flow.FlowKey;
import org.pcap4j.core.*;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;

import java.net.InetAddress;
import java.util.concurrent.*;

public class PacketSniffer {
    private static final int THREAD_POOL_SIZE = 4;
    private static final long FLOW_TIMEOUT = 60_000;

    public static void main(String[] args) throws PcapNativeException, NotOpenException {
        PcapNetworkInterface nif = Pcaps.getDevByName("wlp4s0");
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        ConcurrentHashMap<FlowKey, Flow> flows = new ConcurrentHashMap<>();

        try (PcapHandle handle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 50)) {
            handle.setFilter("ip", BpfProgram.BpfCompileMode.OPTIMIZE);
            
            ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
            cleaner.scheduleAtFixedRate(() -> cleanupFlows(flows), 10, 10, TimeUnit.SECONDS);

            handle.loop(-1, (Packet packet) -> executor.execute(() -> processPacket(packet, flows)));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
            System.err.println("Error processing packet: " + e.getMessage());
        }
    }

    private static void cleanupFlows(ConcurrentHashMap<FlowKey, flow.Flow> flows) {
        long currentTime = System.currentTimeMillis();
        flows.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue().getLastUpdated() > FLOW_TIMEOUT) {
                FeatureSender.sendFeatures(entry.getValue());
                return true;
            }
            return false;
        });
    }
}