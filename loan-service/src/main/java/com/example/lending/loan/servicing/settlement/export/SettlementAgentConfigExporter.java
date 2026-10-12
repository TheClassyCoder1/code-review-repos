package com.example.lending.loan.servicing.settlement.export;

import com.example.lending.loan.servicing.settlement.profiles.SettlementProfile;
import com.example.lending.loan.servicing.settlement.profiles.SettlementProfileRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Regenerates the file agent configuration of every settlement profile once an hour. */
@Component
public class SettlementAgentConfigExporter {

    private final SettlementProfileRegistry profileRegistry;
    private final Path agentConfigDirectory;

    public SettlementAgentConfigExporter(SettlementProfileRegistry profileRegistry,
                                         @Value("${servicing.settlement.agent-config-dir}") String agentConfigDirectory) {
        this.profileRegistry = profileRegistry;
        this.agentConfigDirectory = Path.of(agentConfigDirectory);
    }

    @Scheduled(cron = "${servicing.settlement.agent-config-cron:0 5 * * * *}")
    public void exportAll() {
        for (SettlementProfile profile : profileRegistry.profiles()) {
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("partner.id", profile.getPartnerId());
            config.put("bank.code", profile.getBankCode());
            config.put("currency", profile.getCurrency());
            config.put("cutoff", profile.getCutoffTime());
            config.put("remote.dir", profile.getRemoteDirectory());
            SettlementPropertiesWriter.writeProperties(
                    agentConfigDirectory.resolve(profile.getPartnerId() + ".properties").toString(), config);
        }
    }
}
