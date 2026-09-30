# HGWFF CloudSim 3.0.3 simulation

This project models the paper's HGWFF load-balancing flow for one data center,
50 heterogeneous VMs, and 250 deadline-aware healthcare Cloudlets.

## Run the web dashboard

Install JDK 17+ and Maven, then execute from this directory:

```powershell
mvn spring-boot:run
```

Open `http://localhost:8080` in a browser. The dashboard sends parameters to
the local Java API at `POST /api/simulations`; it does not replace the CloudSim
implementation. `HgwffSimulation` creates the simulation. `HgwffDatacenterBroker` implements
the Firefly smell/vision filtering and the Grey Wolf alpha/beta/delta choice.
`DeadlineCloudlet` supplies the deadline property missing from legacy CloudSim.

The dashboard also has an IoHT -> Fog -> Cloud panel. Cloud task timing and
allocation are actual CloudSim results. The IoHT, fog, cloud-host and network
energy figures are clearly labelled Table 1 projections using
`E(kWh) = power(W) * makespan(s) / 3,600,000`.

The `pom.xml` fetches legacy CloudSim 3.0.3 from JitPack because this artifact
is not published to Maven Central.

## Important modelling note

In CloudSim 3.0.3, `Cloudlet.getFinishTime()` is populated only after the
simulation. The broker therefore uses projected finish time while placing or
replacing a task and prints the literal post-run Equation (12) result using
`task.getFinishTime() < task.getDeadline()`.
