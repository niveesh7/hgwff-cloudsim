package org.example.hgwff;

public class SimulationRequest {
    private int vmCount = 50;
    private int taskCount = 250;
    private int fogNodeCount = 25;
    private int iotNodeCount = 50;
    private double overloadThreshold = 0.010;
    public int getVmCount() { return vmCount; }
    public void setVmCount(int vmCount) { this.vmCount = vmCount; }
    public int getTaskCount() { return taskCount; }
    public void setTaskCount(int taskCount) { this.taskCount = taskCount; }
    public int getFogNodeCount() { return fogNodeCount; }
    public void setFogNodeCount(int fogNodeCount) { this.fogNodeCount = fogNodeCount; }
    public int getIotNodeCount() { return iotNodeCount; }
    public void setIotNodeCount(int iotNodeCount) { this.iotNodeCount = iotNodeCount; }
    public double getOverloadThreshold() { return overloadThreshold; }
    public void setOverloadThreshold(double overloadThreshold) { this.overloadThreshold = overloadThreshold; }
}
