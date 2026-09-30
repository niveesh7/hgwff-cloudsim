package org.example.hgwff;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/** Run with: mvn compile exec:java -Dexec.mainClass=org.example.hgwff.HgwffSimulation */
public final class HgwffSimulation {
    public static void main(String[] args) {
        SimulationResult result = run(new SimulationRequest());
        Log.printLine("HGWFF completed: " + result.getOnTimeTasks() + "/" + result.getTaskCount()
                + " tasks met strict deadlines.");
    }

    /** CloudSim holds global state, so web requests must execute one simulation at a time. */
    public static synchronized SimulationResult run(SimulationRequest request) {
        int vmCount = bounded(request.getVmCount(), 1, 100);
        int taskCount = bounded(request.getTaskCount(), 1, 2_000);
        double threshold = bounded(request.getOverloadThreshold(), 0.001, 1.0);
        CloudSim.init(1, Calendar.getInstance(), false);
        createDatacenter("HealthcareDatacenter"); // Exactly one data center.

        try {
            HgwffDatacenterBroker broker = new HgwffDatacenterBroker("HGWFF_Broker", threshold);
            List<Vm> vms = createVms(broker.getId(), vmCount);
            List<DeadlineCloudlet> tasks = createHealthcareTasks(broker.getId(), taskCount);
            broker.submitHgwffWorkload(vms, tasks);

            CloudSim.startSimulation();
            List<Cloudlet> finished = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();
            return toResult(vmCount, taskCount, threshold, bounded(request.getFogNodeCount(), 1, 100),
                    bounded(request.getIotNodeCount(), 1, 1_000), finished);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to run HGWFF simulation", e);
        }
    }

    private static Datacenter createDatacenter(String name) {
        List<Pe> pes = new ArrayList<Pe>();
        for (int i = 0; i < 100; i++) pes.add(new Pe(i, new PeProvisionerSimple(2_500)));
        List<Host> hosts = new ArrayList<Host>();
        hosts.add(new Host(0, new RamProvisionerSimple(262_144), new BwProvisionerSimple(1_000_000),
                10_000_000, pes, new VmSchedulerTimeShared(pes)));
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hosts, 10.0, 3.0, 0.05, 0.001, 0.0);
        try {
            return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hosts),
                    new LinkedList<Storage>(), 0);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private static List<Vm> createVms(int brokerId, int vmCount) {
        List<Vm> vms = new ArrayList<Vm>();
        for (int i = 0; i < vmCount; i++) {
            double mips = 500 + (i % 10) * 150; // Heterogeneous MIPS: 500..1850.
            vms.add(new Vm(i, brokerId, mips, 1, 2048, 10_000, 10_000, "Xen",
                    new CloudletSchedulerSpaceShared()));
        }
        return vms;
    }

    private static List<DeadlineCloudlet> createHealthcareTasks(int brokerId, int taskCount) {
        List<DeadlineCloudlet> tasks = new ArrayList<DeadlineCloudlet>();
        UtilizationModel full = new UtilizationModelFull();
        for (int i = 0; i < taskCount; i++) {
            long lengthMi = 8_000 + (i % 20) * 750L;
            // Strict deadlines are simulation seconds measured from time 0.
            double deadline = 35.0 + (i % 25) * 4.0;
            DeadlineCloudlet task = new DeadlineCloudlet(i, lengthMi, 1, 300, 300,
                    full, full, full, deadline);
            task.setUserId(brokerId);
            tasks.add(task);
        }
        return tasks;
    }

    private static SimulationResult toResult(int vmCount, int taskCount, double threshold, int fogNodes, int iotNodes, List<Cloudlet> results) {
        int onTime = 0;
        double makespan = 0.0, totalFinishTime = 0.0;
        Map<Integer, Integer> tasksPerVm = new HashMap<Integer, Integer>();
        List<TaskResult> taskResults = new ArrayList<TaskResult>();
        for (Cloudlet cloudlet : results) {
            DeadlineCloudlet task = (DeadlineCloudlet) cloudlet;
            boolean beta = task.getFinishTime() < task.getDeadline();
            if (beta) onTime++;
            makespan = Math.max(makespan, task.getFinishTime());
            totalFinishTime += task.getFinishTime();
            tasksPerVm.put(task.getVmId(), tasksPerVm.getOrDefault(task.getVmId(), 0) + 1);
            taskResults.add(new TaskResult(task.getCloudletId(), task.getVmId(), task.getFinishTime(), task.getDeadline(), beta));
        }
        List<VmResult> vmResults = new ArrayList<VmResult>();
        for (int vmId = 0; vmId < vmCount; vmId++) vmResults.add(new VmResult(vmId, tasksPerVm.getOrDefault(vmId, 0)));
        // Table 1 projection: E(kWh) = P(W) * duration(s) / 3,600,000.
        // CloudSim executes cloud tasks; IoHT, fog and network values are transparent model estimates.
        double hoursFactor = makespan / 3_600_000.0;
        double iotEnergy = iotNodes * 7.0 * hoursFactor;      // OBU processor: 7 W.
        double fogEnergy = fogNodes * 95.0 * hoursFactor;     // Fog server: 95 W.
        double cloudEnergy = 300.0 * hoursFactor;             // Cloud server: 300 W.
        // Table 1 network path total: 3.8 + 5.1 + 64 + 14 + 4.2 + 3.8 kW
        // plus 7.5 W access point and 0.2 W WiFi = 94,907.7 W.
        double networkEnergy = 94_907.7 * hoursFactor;
        return new SimulationResult(vmCount, taskCount, threshold, onTime, taskCount - onTime,
                makespan, totalFinishTime / results.size(), fogNodes, iotNodes, iotEnergy, fogEnergy,
                cloudEnergy, networkEnergy, taskResults, vmResults);
    }

    private static int bounded(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static double bounded(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
}
