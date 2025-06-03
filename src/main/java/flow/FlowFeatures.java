package flow;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

/**
 * Contains statistical features extracted from a network flow.
 * These features can be used for traffic analysis and classification,
 * such as detecting DDoS attacks or other anomalies.
 */
public class FlowFeatures implements Serializable {
    private static final long serialVersionUID = 1L;

    // Protocol and basic flow information
    @JsonProperty("protocol")
    public byte protocol;

    @JsonProperty("source_ip")
    public String sourceIp;

    @JsonProperty("destination_ip")
    public String destinationIp;

    @JsonProperty("source_port")
    public int sourcePort;

    @JsonProperty("destination_port")
    public int destinationPort;

    @JsonProperty("flow_duration")
    public int flowDuration;

    @JsonProperty("total_fwd_packets")
    public int totalFwdPackets;

    @JsonProperty("total_backward_packets")
    public short totalBackwardPackets;

    // Packet length statistics
    @JsonProperty("fwd_packets_length_total")
    public float fwdPacketsLengthTotal;

    @JsonProperty("bwd_packets_length_total")
    public float bwdPacketsLengthTotal;

    @JsonProperty("fwd_packet_length_max")
    public float fwdPacketLengthMax;

    @JsonProperty("fwd_packet_length_min")
    public float fwdPacketLengthMin;

    @JsonProperty("fwd_packet_length_std")
    public float fwdPacketLengthStd;

    @JsonProperty("bwd_packet_length_max")
    public float bwdPacketLengthMax;

    @JsonProperty("bwd_packet_length_min")
    public float bwdPacketLengthMin;

    // Flow rate statistics
    @JsonProperty("flow_bytes_per_s")
    public double flowBytesPerS;

    @JsonProperty("flow_packets_per_s")
    public double flowPacketsPerS;

    @JsonProperty("bwd_packets_per_s")
    public float bwdPacketsPerS;

    // Inter-arrival time (IAT) statistics
    @JsonProperty("flow_iat_mean")
    public float flowIATMean;

    @JsonProperty("flow_iat_min")
    public float flowIATMin;

    @JsonProperty("fwd_iat_total")
    public float fwdIATTotal;

    @JsonProperty("fwd_iat_mean")
    public float fwdIATMean;

    @JsonProperty("fwd_iat_min")
    public float fwdIATMin;

    @JsonProperty("bwd_iat_total")
    public float bwdIATTotal;

    @JsonProperty("bwd_iat_mean")
    public float bwdIATMean;

    @JsonProperty("bwd_iat_min")
    public float bwdIATMin;

    // Header information
    @JsonProperty("fwd_header_length")
    public long fwdHeaderLength;

    @JsonProperty("bwd_header_length")
    public long bwdHeaderLength;

    // Packet statistics
    @JsonProperty("packet_length_max")
    public float packetLengthMax;

    @JsonProperty("packet_length_mean")
    public float packetLengthMean;

    // TCP flag counts
    @JsonProperty("syn_flag_count")
    public byte synFlagCount;

    @JsonProperty("ack_flag_count")
    public byte ackFlagCount;

    @JsonProperty("urg_flag_count")
    public byte urgFlagCount;

    // Ratio statistics
    @JsonProperty("down_up_ratio")
    public float downUpRatio;

    // Activity statistics
    @JsonProperty("active_mean")
    public float activeMean;

    @JsonProperty("active_std")
    public float activeStd;

    @JsonProperty("active_max")
    public float activeMax;

    @JsonProperty("active_min")
    public float activeMin;

    // Idle statistics
    @JsonProperty("idle_mean")
    public float idleMean;

    @JsonProperty("idle_std")
    public float idleStd;

    @JsonProperty("idle_max")
    public float idleMax;

    @Override
    public String toString() {
        return "FlowFeatures{" +
                "protocol=" + protocol +
                ", sourceIp='" + sourceIp + '\'' +
                ", destinationIp='" + destinationIp + '\'' +
                ", sourcePort=" + sourcePort +
                ", destinationPort=" + destinationPort +
                ", flowDuration=" + flowDuration +
                ", totalFwdPackets=" + totalFwdPackets +
                ", totalBackwardPackets=" + totalBackwardPackets +
                ", fwdPacketsLengthTotal=" + fwdPacketsLengthTotal +
                ", bwdPacketsLengthTotal=" + bwdPacketsLengthTotal +
                ", flowBytesPerS=" + flowBytesPerS +
                ", flowPacketsPerS=" + flowPacketsPerS +
                ", synFlagCount=" + synFlagCount +
                ", ackFlagCount=" + ackFlagCount +
                ", urgFlagCount=" + urgFlagCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowFeatures that = (FlowFeatures) o;
        return protocol == that.protocol &&
                sourcePort == that.sourcePort &&
                destinationPort == that.destinationPort &&
                flowDuration == that.flowDuration &&
                totalFwdPackets == that.totalFwdPackets &&
                totalBackwardPackets == that.totalBackwardPackets &&
                Float.compare(that.fwdPacketsLengthTotal, fwdPacketsLengthTotal) == 0 &&
                Float.compare(that.bwdPacketsLengthTotal, bwdPacketsLengthTotal) == 0 &&
                Double.compare(that.flowBytesPerS, flowBytesPerS) == 0 &&
                Double.compare(that.flowPacketsPerS, flowPacketsPerS) == 0 &&
                synFlagCount == that.synFlagCount &&
                ackFlagCount == that.ackFlagCount &&
                urgFlagCount == that.urgFlagCount &&
                Objects.equals(sourceIp, that.sourceIp) &&
                Objects.equals(destinationIp, that.destinationIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, sourceIp, destinationIp, sourcePort, destinationPort,
                flowDuration, totalFwdPackets, totalBackwardPackets,
                fwdPacketsLengthTotal, bwdPacketsLengthTotal, flowBytesPerS, flowPacketsPerS,
                synFlagCount, ackFlagCount, urgFlagCount);
    }
}
