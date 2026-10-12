package com.example.lending.loan.servicing.settlement.profiles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Settlement profiles from the profile directory, reloaded every few minutes. */
@Component
public class SettlementProfileRegistry {

    private static final Logger log = LoggerFactory.getLogger(SettlementProfileRegistry.class);

    private static final Pattern PARTNER_ID = Pattern.compile("^[a-z0-9-]{1,64}$");

    private final Path profileDirectory;
    private final Map<String, SettlementProfile> profiles = new ConcurrentHashMap<>();

    public SettlementProfileRegistry(@Value("${servicing.settlement.profiles-dir}") String profileDirectory) {
        this.profileDirectory = Path.of(profileDirectory);
    }

    @Scheduled(fixedDelayString = "${servicing.settlement.profiles-reload-ms:300000}", initialDelay = 0)
    public void reload() {
        if (!Files.isDirectory(profileDirectory)) {
            log.warn("Settlement profile directory {} does not exist", profileDirectory);
            return;
        }
        try (Stream<Path> files = Files.list(profileDirectory)) {
            files.filter(file -> file.getFileName().toString().endsWith(".yml")).forEach(file -> {
                SettlementProfile profile = SettlementProfileLoader.readYaml(file.toString(), SettlementProfile.class);
                if (profile != null && profile.getPartnerId() != null
                        && PARTNER_ID.matcher(profile.getPartnerId()).matches()) {
                    profiles.put(profile.getPartnerId(), profile);
                }
            });
        } catch (IOException e) {
            log.warn("Cannot list settlement profiles: {}", e.toString());
        }
    }

    public List<SettlementProfile> profiles() {
        return List.copyOf(profiles.values());
    }

    public SettlementProfile get(String partnerId) {
        return profiles.get(partnerId);
    }
}
