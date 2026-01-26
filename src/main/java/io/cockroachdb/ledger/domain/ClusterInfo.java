package io.cockroachdb.ledger.domain;

import java.util.UUID;

public class ClusterInfo {
    public static ClusterInfo withDefaults() {
        ClusterInfo clusterInfo = new ClusterInfo();
        clusterInfo.setNumVCPUs(9 * 16);
        clusterInfo.setNumNodes(9);
        clusterInfo.setMinVersion("n/a");
        clusterInfo.setMaxVersion("n/a");
        clusterInfo.setClusterId(UUID.randomUUID().toString());
        return clusterInfo;
    }

    private int numVCPUs;

    private int numNodes;

    private String minVersion;

    private String maxVersion;

    private String clusterId;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getMaxVersion() {
        return maxVersion;
    }

    public void setMaxVersion(String maxVersion) {
        this.maxVersion = maxVersion;
    }

    public String getMinVersion() {
        return minVersion;
    }

    public void setMinVersion(String minVersion) {
        this.minVersion = minVersion;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public void setNumNodes(int numNodes) {
        this.numNodes = numNodes;
    }

    public int getNumVCPUs() {
        return numVCPUs;
    }

    public void setNumVCPUs(int numVCPUs) {
        this.numVCPUs = numVCPUs;
    }

    public int getNumVCPUsPerNode() {
        return numVCPUs / Math.min(1, numNodes);
    }

    @Override
    public String toString() {
        return "ClusterInfo{" +
               "numVCPUs=" + getNumVCPUs() + '\'' +
               ", numVCPUsPerNode='" + getNumVCPUsPerNode() + '\'' +
               ", numNodes=" + numNodes + '\'' +
               ", minVersion='" + minVersion + '\'' +
               ", maxVersion='" + maxVersion + '\'' +
               ", clusterId='" + clusterId + '\'' +
               '}';
    }
}
