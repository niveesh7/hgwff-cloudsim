package org.example.hgwff;

import org.springframework.http.ResponseEntity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {
    @PostMapping
    public ResponseEntity<SimulationResult> run(@RequestBody(required = false) SimulationRequest request) {
        return ResponseEntity.ok(HgwffSimulation.run(request == null ? new SimulationRequest() : request));
    }

    /** Four load points, similar to the paper's task-load figures, using the selected VM parameters. */
    @PostMapping("/experiment")
    public ResponseEntity<List<SimulationResult>> experiment(@RequestBody(required = false) SimulationRequest request) {
        SimulationRequest base = request == null ? new SimulationRequest() : request;
        int[] loads = {Math.max(1, base.getTaskCount() / 4), Math.max(1, base.getTaskCount() / 2),
                Math.max(1, base.getTaskCount() * 3 / 4), base.getTaskCount()};
        List<SimulationResult> results = new ArrayList<SimulationResult>();
        for (int load : loads) {
            SimulationRequest point = new SimulationRequest();
            point.setVmCount(base.getVmCount()); point.setTaskCount(load);
            point.setFogNodeCount(base.getFogNodeCount()); point.setIotNodeCount(base.getIotNodeCount());
            point.setOverloadThreshold(base.getOverloadThreshold());
            results.add(HgwffSimulation.run(point));
        }
        return ResponseEntity.ok(results);
    }
}
