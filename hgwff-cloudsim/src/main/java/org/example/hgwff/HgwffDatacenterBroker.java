package org.example.hgwff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;

/**
 * A CloudSim 3.0.3 broker that plans HGWFF placements before submitting
 * Cloudlets to the data center.  This is the appropriate point to "migrate"
 * a task: after a Cloudlet is running, legacy CloudSim has no portable live
 * migration API for Cloudlets.
 */
public final class HgwffDatacenterBroker extends DatacenterBroker {
    private static final int GWO_ITERATIONS = 30;
    private final Random random = new Random(20260916L);
    private final double overloadThreshold;
    private final Map<Integer, VmState> stateByVmId = new HashMap<Integer, VmState>();

    public HgwffDatacenterBroker(String name, double overloadThreshold) throws Exception {
        super(name);
        this.overloadThreshold = overloadThreshold;
    }

    /**
     * Runs Algorithm 1's Firefly -> Grey Wolf decision before normal broker
     * submission.  Cloudlet-to-VM bindings are then consumed by CloudSim.
     */
    public void submitHgwffWorkload(List<Vm> vms, List<DeadlineCloudlet> cloudlets) {
        for (Vm vm : vms) {
            stateByVmId.put(vm.getId(), new VmState(vm));
        }

        // DatacenterBroker.bindCloudletToVm searches its submitted Cloudlet
        // list. Register both lists before creating the HGWFF bindings.
        submitVmList(vms);
        submitCloudletList(new ArrayList<Cloudlet>(cloudlets));

        // Earliest-deadline-first makes the paper's strict deadline objective explicit.
        List<DeadlineCloudlet> ordered = new ArrayList<DeadlineCloudlet>(cloudlets);
        ordered.sort(Comparator.comparingDouble(DeadlineCloudlet::getDeadline));

        for (DeadlineCloudlet task : ordered) {
            placeTask(task);
        }
    }

    private void placeTask(DeadlineCloudlet task) {
        // First try an unconstrained, least-loaded VM. It represents the initial fly position.
        VmState source = leastLoaded(stateByVmId.values());
        source.add(task);

        // Equation (6): L(V_j,t) = N(T,t) / SR(V_j,t).
        // Equation (8): T(V_j) = L(V_j,t) / C(V_j).
        boolean overloaded = source.load() > overloadThreshold;
        double predictedFinish = source.projectedFinish(task);

        // Equation (12): beta_ij = 1 iff f_i < d_i.  Otherwise 0 -> migrate.
        boolean deadlineMiss = !meetsDecisionVariable(predictedFinish, task.getDeadline());
        if (!overloaded && !deadlineMiss) {
            bindCloudletToVm(task.getCloudletId(), source.vm.getId());
            return;
        }

        // "Remove the task" from the overloaded/deadline-missing candidate.
        source.remove(task);

        // Firefly phase: smell discovers accessible VMs; vision filters viable VMs.
        List<VmState> smelled = smellAccessibleVms(task, source);
        List<VmState> visible = visionShortlist(task, smelled);

        // Grey Wolf phase: alpha, beta, delta guide the final candidate selection.
        VmState best = greyWolfSelect(task, visible.isEmpty() ? smelled : visible);
        if (best == null) {
            // No feasible deadline candidate: still schedule on the best available VM and report it.
            best = leastLoaded(stateByVmId.values());
            Log.printLine("HGWFF: Cloudlet " + task.getCloudletId()
                    + " has no deadline-feasible VM; assigning best-effort VM " + best.vm.getId());
        }
        best.add(task);
        bindCloudletToVm(task.getCloudletId(), best.vm.getId());
    }

    private boolean meetsDecisionVariable(double finishTime, double deadline) {
        return finishTime < deadline; // Equation (12), deliberately strict as in the paper.
    }

    /** Olfactory/smell search: VMs reachable to this broker and not the source. */
    private List<VmState> smellAccessibleVms(DeadlineCloudlet task, VmState source) {
        List<VmState> result = new ArrayList<VmState>();
        for (VmState candidate : stateByVmId.values()) {
            if (candidate != source && candidate.vm.getNumberOfPes() >= task.getNumberOfPes()) {
                result.add(candidate);
            }
        }
        return result;
    }

    /** Visual search: retain threshold-safe VMs satisfying the Eq. (12) deadline check. */
    private List<VmState> visionShortlist(DeadlineCloudlet task, List<VmState> candidates) {
        List<VmState> result = new ArrayList<VmState>();
        for (VmState candidate : candidates) {
            if (candidate.loadAfter(task) <= overloadThreshold
                    && meetsDecisionVariable(candidate.projectedFinishAfter(task), task.getDeadline())) {
                result.add(candidate);
            }
        }
        return result;
    }

    private VmState greyWolfSelect(DeadlineCloudlet task, List<VmState> candidates) {
        if (candidates.isEmpty()) return null;
        List<Wolf> wolves = new ArrayList<Wolf>();
        for (VmState vm : candidates) wolves.add(new Wolf(vm, fitness(vm, task)));

        // Algorithm 1: update candidate positions guided by alpha/beta/delta, lowering a from 2 to 0.
        for (int iteration = 0; iteration < GWO_ITERATIONS; iteration++) {
            wolves.sort(Comparator.comparingDouble(w -> w.fitness));
            Wolf alpha = wolves.get(0);
            Wolf beta = wolves.get(Math.min(1, wolves.size() - 1));
            Wolf delta = wolves.get(Math.min(2, wolves.size() - 1));
            double a = 2.0 - (2.0 * iteration / (double) GWO_ITERATIONS);

            for (Wolf wolf : wolves) {
                // One-dimensional discrete position: index of a candidate VM.
                double x = candidates.indexOf(wolf.vm);
                double x1 = gwoPosition(alpha, x, a, candidates);
                double x2 = gwoPosition(beta, x, a, candidates);
                double x3 = gwoPosition(delta, x, a, candidates);
                int index = (int) Math.round((x1 + x2 + x3) / 3.0);
                index = Math.max(0, Math.min(candidates.size() - 1, index));
                wolf.vm = candidates.get(index);
                wolf.fitness = fitness(wolf.vm, task);
            }
        }
        wolves.sort(Comparator.comparingDouble(w -> w.fitness));
        return wolves.get(0).vm; // alpha wolf: absolute optimum in the final population.
    }

    private double gwoPosition(Wolf leader, double current, double a, List<VmState> candidates) {
        double leaderPosition = candidates.indexOf(leader.vm);
        double r1 = random.nextDouble(), r2 = random.nextDouble();
        double A = 2.0 * a * r1 - a;
        double C = 2.0 * r2;
        double distance = Math.abs(C * leaderPosition - current);
        return leaderPosition - A * distance;
    }

    /** Lower is better: deadline slack, paper Eq. (6) load, and Eq. (8) processing time. */
    private double fitness(VmState vm, DeadlineCloudlet task) {
        double finish = vm.projectedFinishAfter(task);
        double latenessPenalty = Math.max(0.0, finish - task.getDeadline()) * 1_000_000.0;
        return latenessPenalty + vm.loadAfter(task) + vm.processingTimeAfter(task);
    }

    private VmState leastLoaded(Collection<VmState> states) {
        return states.stream().min(Comparator.comparingDouble(VmState::load)).get();
    }

    private static final class Wolf {
        private VmState vm;
        private double fitness;
        private Wolf(VmState vm, double fitness) { this.vm = vm; this.fitness = fitness; }
    }

    private static final class VmState {
        private final Vm vm;
        private final List<DeadlineCloudlet> tasks = new ArrayList<DeadlineCloudlet>();
        private double queuedMi;

        private VmState(Vm vm) { this.vm = vm; }
        private void add(DeadlineCloudlet task) { tasks.add(task); queuedMi += task.getCloudletLength(); }
        private void remove(DeadlineCloudlet task) { tasks.remove(task); queuedMi -= task.getCloudletLength(); }
        private double capacity() { return vm.getMips() * vm.getNumberOfPes(); } // Eq. (5) simplified to CloudSim CPU capacity.
        private double serviceRate() { return capacity(); }
        private double load() { return tasks.size() / serviceRate(); } // Equation (6).
        private double loadAfter(DeadlineCloudlet task) { return (tasks.size() + 1.0) / serviceRate(); }
        private double processingTime() { return load() / capacity(); } // Equation (8).
        private double processingTimeAfter(DeadlineCloudlet task) { return loadAfter(task) / capacity(); }
        private double projectedFinish(DeadlineCloudlet task) { return queuedMi / capacity(); }
        private double projectedFinishAfter(DeadlineCloudlet task) { return (queuedMi + task.getCloudletLength()) / capacity(); }
    }
}
