package org.example.hgwff;

import java.util.List;

public class SimulationResult {
    private final int vmCount, taskCount, onTimeTasks, missedTasks;
    private final double overloadThreshold, makespan, averageFinishTime;
    private final int fogNodeCount, iotNodeCount;
    private final double iotEnergyKwh, fogEnergyKwh, cloudEnergyKwh, networkEnergyKwh;
    private final List<TaskResult> tasks;
    private final List<VmResult> vms;
    public SimulationResult(int vmCount, int taskCount, double overloadThreshold, int onTimeTasks, int missedTasks,
                            double makespan, double averageFinishTime, int fogNodeCount, int iotNodeCount,
                            double iotEnergyKwh, double fogEnergyKwh, double cloudEnergyKwh, double networkEnergyKwh,
                            List<TaskResult> tasks, List<VmResult> vms) {
        this.vmCount=vmCount; this.taskCount=taskCount; this.overloadThreshold=overloadThreshold;
        this.onTimeTasks=onTimeTasks; this.missedTasks=missedTasks; this.makespan=makespan; this.averageFinishTime=averageFinishTime; this.fogNodeCount=fogNodeCount; this.iotNodeCount=iotNodeCount; this.iotEnergyKwh=iotEnergyKwh; this.fogEnergyKwh=fogEnergyKwh; this.cloudEnergyKwh=cloudEnergyKwh; this.networkEnergyKwh=networkEnergyKwh; this.tasks=tasks; this.vms=vms;
    }
    public int getVmCount() { return vmCount; } public int getTaskCount() { return taskCount; }
    public double getOverloadThreshold() { return overloadThreshold; } public int getOnTimeTasks() { return onTimeTasks; }
    public int getMissedTasks() { return missedTasks; } public double getMakespan() { return makespan; }
    public double getAverageFinishTime() { return averageFinishTime; }
    public int getFogNodeCount() { return fogNodeCount; } public int getIotNodeCount() { return iotNodeCount; }
    public double getIotEnergyKwh() { return iotEnergyKwh; } public double getFogEnergyKwh() { return fogEnergyKwh; }
    public double getCloudEnergyKwh() { return cloudEnergyKwh; } public double getNetworkEnergyKwh() { return networkEnergyKwh; }
    public List<TaskResult> getTasks() { return tasks; } public List<VmResult> getVms() { return vms; }
}
