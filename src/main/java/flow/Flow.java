package flow;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Flow {
    // Timing and state management
    private final AtomicLong startTime = new AtomicLong(-1);
    private final AtomicLong lastUpdated = new AtomicLong();
    private long lastPacketTime = 0;

    // Packet storage for statistical calculations
    private final List<Float> fwdPacketLengths = new CopyOnWriteArrayList<>();
    private final List<Float> bwdPacketLengths = new CopyOnWriteArrayList<>();
    private final List<Long> fwdIats = new CopyOnWriteArrayList<>();
    private final List<Long> bwdIats = new CopyOnWriteArrayList<>();
    private final List<Long> flowIats = new CopyOnWriteArrayList<>();
    private final List<Long> activeTimes = new CopyOnWriteArrayList<>();
    private final List<Long> idleTimes = new CopyOnWriteArrayList<>();

    // Counters and flags
    private final AtomicInteger totalFwdPackets = new AtomicInteger();
    private final AtomicInteger totalBwdPackets = new AtomicInteger();
    private final AtomicInteger synCount = new AtomicInteger();
    private final AtomicInteger ackCount = new AtomicInteger();
    private final AtomicInteger urgCount = new AtomicInteger();
    private final AtomicLong fwdHeaderLength = new AtomicLong();
    private final AtomicLong bwdHeaderLength = new AtomicLong();

    // Extreme values tracking
    private final AtomicReference<Float> fwdMaxLength = new AtomicReference<>(Float.MIN_VALUE);
    private final AtomicReference<Float> fwdMinLength = new AtomicReference<>(Float.MAX_VALUE);
    private final AtomicReference<Float> bwdMaxLength = new AtomicReference<>(Float.MIN_VALUE);
    private final AtomicReference<Float> bwdMinLength = new AtomicReference<>(Float.MAX_VALUE);
    private final AtomicReference<Float> flowIatMin = new AtomicReference<>(Float.MAX_VALUE);

    // flow.Flow identification
    private InetAddress srcIp;
    private InetAddress dstIp;
    private int srcPort;
    private int dstPort;
    private int protocol;

    public void initialize(InetAddress srcIp, InetAddress dstIp, int srcPort, int dstPort, int protocol) {
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.protocol = protocol;
    }

    public synchronized void update(Packet packet) {
        long currentTime = System.currentTimeMillis();
        if (startTime.get() == -1) {
            startTime.set(currentTime);
            lastPacketTime = currentTime;
        }

        boolean isForward = isForwardPacket(packet);
        float length = packet.length();

        updatePacketMetrics(isForward, length);
        updateTimingMetrics(currentTime, isForward);
        updateHeaderMetrics(packet, isForward);
        updateFlagMetrics(packet);
        updateActivityMetrics(currentTime);

        lastPacketTime = currentTime;
        lastUpdated.set(currentTime);
    }

    private boolean isForwardPacket(Packet packet) {
        IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
        InetAddress packetSrcIp = ipV4Packet.getHeader().getSrcAddr();
        InetAddress packetDstIp = ipV4Packet.getHeader().getDstAddr();

        // Handle different protocol types
        if (packet.contains(TcpPacket.class)) {
            TcpPacket tcpPacket = packet.get(TcpPacket.class);
            int packetSrcPort = tcpPacket.getHeader().getSrcPort().valueAsInt();
            int packetDstPort = tcpPacket.getHeader().getDstPort().valueAsInt();

            return packetSrcIp.equals(srcIp) &&
                    packetDstIp.equals(dstIp) &&
                    packetSrcPort == srcPort &&
                    packetDstPort == dstPort;
        } else if (packet.contains(UdpPacket.class)) {
            UdpPacket udpPacket = packet.get(UdpPacket.class);
            int packetSrcPort = udpPacket.getHeader().getSrcPort().valueAsInt();
            int packetDstPort = udpPacket.getHeader().getDstPort().valueAsInt();

            return packetSrcIp.equals(srcIp) &&
                    packetDstIp.equals(dstIp) &&
                    packetSrcPort == srcPort &&
                    packetDstPort == dstPort;
        } else if (packet.contains(IcmpV4CommonPacket.class)) {
            // For ICMP, direction is determined by IP addresses only
            return packetSrcIp.equals(srcIp) && packetDstIp.equals(dstIp);
        }

        // Default to source IP check for other protocols
        return packetSrcIp.equals(srcIp);
    }

    private void updatePacketMetrics(boolean isForward, float length) {
        if (isForward) {
            totalFwdPackets.incrementAndGet();
            fwdPacketLengths.add(length);
            fwdMaxLength.accumulateAndGet(length, Math::max);
            fwdMinLength.accumulateAndGet(length, Math::min);
        } else {
            totalBwdPackets.incrementAndGet();
            bwdPacketLengths.add(length);
            bwdMaxLength.accumulateAndGet(length, Math::max);
            bwdMinLength.accumulateAndGet(length, Math::min);
        }
    }

    private void updateTimingMetrics(long currentTime, boolean isForward) {
        long iat = currentTime - lastPacketTime;
        if (iat > 0) {
            flowIats.add(iat);
            flowIatMin.accumulateAndGet((float) iat, Math::min);

            if (isForward) {
                fwdIats.add(iat);
            } else {
                bwdIats.add(iat);
            }
        }
    }

    private void updateHeaderMetrics(Packet packet, boolean isForward) {
        if (packet.contains(TcpPacket.class)) {
            TcpPacket tcpPacket = packet.get(TcpPacket.class);
            int headerLength = tcpPacket.getHeader().getDataOffset() * 4;

            if (isForward) {
                fwdHeaderLength.addAndGet(headerLength);
            } else {
                bwdHeaderLength.addAndGet(headerLength);
            }
        }
    }

    private void updateFlagMetrics(Packet packet) {
        if (packet.contains(TcpPacket.class)) {
            TcpPacket tcpPacket = packet.get(TcpPacket.class);
            byte flags = tcpPacket.getHeader().getRawData()[13]; // TCP flags are in byte 13

            // Check SYN flag (bit 1, value 0x02)
            if ((flags & 0x02) != 0) {
                synCount.incrementAndGet();
            }

            // Check ACK flag (bit 4, value 0x10)
            if ((flags & 0x10) != 0) {
                ackCount.incrementAndGet();
            }

            // Check URG flag (bit 5, value 0x20)
            if ((flags & 0x20) != 0) {
                urgCount.incrementAndGet();
            }
        }
    }

    private void updateActivityMetrics(long currentTime) {
        long elapsed = currentTime - lastPacketTime;
        if (elapsed > 0) {
            if (elapsed > 1000) { // 1 second threshold for idle time
                idleTimes.add(elapsed);
            } else {
                activeTimes.add(elapsed);
            }
        }
    }

    public synchronized FlowFeatures getFeatures() {
        FlowFeatures features = new FlowFeatures();
        long durationMillis = lastUpdated.get() - startTime.get();

        // Basic features
        features.protocol = (byte) protocol;
        features.flowDuration = (int) (durationMillis * 1000);
        features.totalFwdPackets = totalFwdPackets.get();
        features.totalBackwardPackets = (short) totalBwdPackets.get();

        // Packet length statistics
        features.fwdPacketsLengthTotal = sumFloatList(fwdPacketLengths);
        features.bwdPacketsLengthTotal = sumFloatList(bwdPacketLengths);
        features.fwdPacketLengthMax = fwdMaxLength.get();
        features.fwdPacketLengthMin = fwdMinLength.get();
        features.fwdPacketLengthStd = calculateStdDev(fwdPacketLengths);
        features.bwdPacketLengthMax = bwdMaxLength.get();
        features.bwdPacketLengthMin = bwdMinLength.get();

        // IAT statistics
        features.flowIATMean = calculateMean(flowIats);
        features.flowIATMin = flowIatMin.get();
        features.fwdIATTotal = sumLongList(fwdIats);
        features.fwdIATMean = calculateMean(fwdIats);
        features.fwdIATMin = minLongList(fwdIats);
        features.bwdIATTotal = sumLongList(bwdIats);
        features.bwdIATMean = calculateMean(bwdIats);
        features.bwdIATMin = minLongList(bwdIats);

        // Header lengths
        features.fwdHeaderLength = fwdHeaderLength.get();
        features.bwdHeaderLength = bwdHeaderLength.get();

        // Rate calculations
        float durationSeconds = durationMillis / 1000.0f;
        features.flowBytesPerS = (double) (features.fwdPacketsLengthTotal + features.bwdPacketsLengthTotal) / durationSeconds;
        features.flowPacketsPerS = (double) (totalFwdPackets.get() + totalBwdPackets.get()) / durationSeconds;
        features.bwdPacketsPerS = totalBwdPackets.get() / durationSeconds;

        // Packet length stats
        features.packetLengthMax = Math.max(features.fwdPacketLengthMax, features.bwdPacketLengthMax);
        features.packetLengthMean = (features.fwdPacketsLengthTotal + features.bwdPacketsLengthTotal) /
                (totalFwdPackets.get() + totalBwdPackets.get());

        // Flags
        features.synFlagCount = (byte) synCount.get();
        features.ackFlagCount = (byte) ackCount.get();
        features.urgFlagCount = (byte) urgCount.get();

        // Ratios
        features.downUpRatio = totalBwdPackets.get() / (float) Math.max(1, totalFwdPackets.get());

        // Activity statistics
        features.activeMean = calculateMean(activeTimes);
        features.activeStd = calculateStdDevLong(activeTimes);
        features.activeMax = maxLongList(activeTimes);
        features.activeMin = minLongList(activeTimes);

        // Idle statistics
        features.idleMean = calculateMean(idleTimes);
        features.idleStd = calculateStdDevLong(idleTimes);
        features.idleMax = maxLongList(idleTimes);

        return features;
    }

    public long getLastUpdated() {
        return lastUpdated.get();
    }

    // Helper methods
    private float sumFloatList(List<Float> list) {
        return (float) list.stream().mapToDouble(Float::doubleValue).sum();
    }

    private long sumLongList(List<Long> list) {
        return list.stream().mapToLong(Long::longValue).sum();
    }

    private float calculateMean(List<Long> list) {
        return list.isEmpty() ? 0 : (float) list.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private float minLongList(List<Long> list) {
        return list.isEmpty() ? 0 : (float) list.stream().mapToLong(Long::longValue).min().orElse(0);
    }

    private float maxLongList(List<Long> list) {
        return list.isEmpty() ? 0 : (float) list.stream().mapToLong(Long::longValue).max().orElse(0);
    }

    private float calculateStdDev(List<Float> list) {
        return list.isEmpty() ? 0 : (float) new StandardDeviation().evaluate(
                list.stream().mapToDouble(Float::doubleValue).toArray()
        );
    }

    private float calculateStdDevLong(List<Long> list) {
        return list.isEmpty() ? 0 : (float) new StandardDeviation().evaluate(
                list.stream().mapToDouble(Long::doubleValue).toArray()
        );
    }
}