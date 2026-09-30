package org.example.hgwff;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

/** CloudSim 3.x has no deadline field, so this subtype carries the paper's d_i. */
public final class DeadlineCloudlet extends Cloudlet {
    private final double deadline;

    public DeadlineCloudlet(int id, long length, int pes, long fileSize, long outputSize,
                            UtilizationModel cpu, UtilizationModel ram, UtilizationModel bw,
                            double deadline) {
        super(id, length, pes, fileSize, outputSize, cpu, ram, bw);
        this.deadline = deadline;
    }

    public double getDeadline() {
        return deadline;
    }
}
