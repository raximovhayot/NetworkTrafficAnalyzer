package flow;

import java.net.InetAddress;

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

    private boolean shouldSwap(InetAddress srcIp, InetAddress dstIp, int srcPort, int dstPort) {
        int ipComparison = srcIp.getHostAddress().compareTo(dstIp.getHostAddress());
        if (ipComparison > 0) return true;
        if (ipComparison == 0) {
            return srcPort > dstPort;
        }
        return false;
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

}