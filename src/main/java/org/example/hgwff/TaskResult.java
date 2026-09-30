package org.example.hgwff;
public class TaskResult {
    private final int taskId, vmId; private final double finishTime, deadline; private final boolean onTime;
    public TaskResult(int taskId, int vmId, double finishTime, double deadline, boolean onTime) {
        this.taskId=taskId; this.vmId=vmId; this.finishTime=finishTime; this.deadline=deadline; this.onTime=onTime;
    }
    public int getTaskId() { return taskId; } public int getVmId() { return vmId; }
    public double getFinishTime() { return finishTime; } public double getDeadline() { return deadline; }
    public boolean isOnTime() { return onTime; }
}
