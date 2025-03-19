package flow;

import java.net.InetAddress;
import java.util.Objects;

/**
 * Represents a unique identifier for a network flow.
 * Normalizes direction by sorting IP/port pairs to ensure consistent identification
 * regardless of the direction of capture.
 */
public record FlowKey(InetAddress srcIp, InetAddress dstIp, int srcPort, int dstPort, int protocol) {
    public FlowKey(InetAddress srcIp, InetAddress dstIp, int srcPort, int dstPort, int protocol) {
        // Normalize direction by sorting IP/port pairs
        if (shouldSwap(srcIp, dstIp, srcPort, dstPort)) {
            this.srcIp = dstIp;
            this.dstIp = srcIp;
            this.srcPort = dstPort;
            this.dstPort = srcPort;
        } else {
            this.srcIp = srcIp;
            this.dstIp = dstIp;
            this.srcPort = srcPort;
            this.dstPort = dstPort;
        }
        this.protocol = protocol;
    }

    /**
     * Determines if the source and destination should be swapped for normalization.
     * This ensures that flows are consistently identified regardless of direction.
     * 
     * @param srcIp source IP address
     * @param dstIp destination IP address
     * @param srcPort source port
     * @param dstPort destination port
     * @return true if the source and destination should be swapped
     */
    private boolean shouldSwap(InetAddress srcIp, InetAddress dstIp, int srcPort, int dstPort) {
        // Compare IP addresses as byte arrays for better performance
        byte[] srcBytes = srcIp.getAddress();
        byte[] dstBytes = dstIp.getAddress();

        // Compare byte by byte
        for (int i = 0; i < srcBytes.length && i < dstBytes.length; i++) {
            int srcByte = srcBytes[i] & 0xFF;  // Convert to unsigned
            int dstByte = dstBytes[i] & 0xFF;  // Convert to unsigned

            if (srcByte != dstByte) {
                return srcByte > dstByte;
            }
        }

        // If IP addresses are equal, compare ports
        if (srcBytes.length == dstBytes.length) {
            return srcPort > dstPort;
        }

        // If IP address lengths differ (IPv4 vs IPv6), use the longer one as "greater"
        return srcBytes.length > dstBytes.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowKey flowKey = (FlowKey) o;
        return srcPort == flowKey.srcPort &&
                dstPort == flowKey.dstPort &&
                protocol == flowKey.protocol &&
                srcIp.equals(flowKey.srcIp) &&
                dstIp.equals(flowKey.dstIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcIp, dstIp, srcPort, dstPort, protocol);
    }

    @Override
    public String toString() {
        return String.format("FlowKey[%s:%d <-> %s:%d, protocol=%d]", 
                srcIp.getHostAddress(), srcPort, 
                dstIp.getHostAddress(), dstPort, 
                protocol);
    }
}
