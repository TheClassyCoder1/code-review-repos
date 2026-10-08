package com.example.lending.loan.scoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Holds the what-if scenario analysts are currently running against incoming applications. */
@Service
public class ScenarioService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioService.class);

    private final AtomicReference<RiskScenario> active = new AtomicReference<>();

    public RiskScenario restore(byte[] snapshot) {
        RiskScenario scenario;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(snapshot))) {
            scenario = (RiskScenario) in.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scenario snapshot could not be read");
        }
        if (scenario == null || scenario.getName() == null || scenario.getMaxDebtToIncome() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scenario snapshot is incomplete");
        }
        active.set(scenario);
        log.info("Restored risk scenario with cutoff {}", scenario.getCutoffScore());
        return scenario;
    }

    public Optional<RiskScenario> activeScenario() {
        return Optional.ofNullable(active.get());
    }
}
