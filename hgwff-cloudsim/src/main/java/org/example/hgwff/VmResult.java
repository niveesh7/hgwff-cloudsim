package org.example.hgwff;
public class VmResult {
    private final int vmId, assignedTasks;
    public VmResult(int vmId, int assignedTasks) { this.vmId=vmId; this.assignedTasks=assignedTasks; }
    public int getVmId() { return vmId; } public int getAssignedTasks() { return assignedTasks; }
}
