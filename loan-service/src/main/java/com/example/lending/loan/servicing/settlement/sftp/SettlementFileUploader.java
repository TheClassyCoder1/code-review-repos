package com.example.lending.loan.servicing.settlement.sftp;

import com.example.lending.loan.servicing.settlement.sftp.SettlementSftpSessions.SettlementSftpSettings;
import com.example.lending.loan.servicing.settlement.profiles.SettlementProfile;
import com.example.lending.loan.servicing.settlement.profiles.SettlementProfileRegistry;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/** Uploads generated settlement files to the clearing bank, one task per partner in parallel. */
@Component
@EnableConfigurationProperties(SettlementSftpSettings.class)
public class SettlementFileUploader {

    private static final Logger log = LoggerFactory.getLogger(SettlementFileUploader.class);

    private final SettlementProfileRegistry profiles;
    private final Path outboxDirectory;
    private final ExecutorService uploadPool = Executors.newFixedThreadPool(4);

    public SettlementFileUploader(SettlementSftpSettings settings, SettlementProfileRegistry profiles,
                                  @Value("${servicing.settlement.outbox-dir}") String outboxDirectory) {
        SettlementSftpSessions.configure(settings);
        this.profiles = profiles;
        this.outboxDirectory = Path.of(outboxDirectory);
    }

    @Scheduled(cron = "${servicing.settlement.upload-cron:0 */10 * * * *}")
    public void uploadAll() {
        for (SettlementProfile profile : profiles.profiles()) {
            uploadPool.submit(() -> uploadPartner(profile));
        }
    }

    void uploadPartner(SettlementProfile profile) {
        Path partnerOutbox = outboxDirectory.resolve(profile.getPartnerId());
        List<Path> files = listFiles(partnerOutbox);
        if (files.isEmpty()) {
            return;
        }
        ChannelSftp channel = null;
        try {
            channel = (ChannelSftp) SettlementSftpSessions.sftpSession().openChannel("sftp");
            channel.connect();
            channel.cd(profile.getRemoteDirectory());
            Files.createDirectories(partnerOutbox.resolve("sent"));
            for (Path file : files) {
                try (InputStream in = Files.newInputStream(file)) {
                    channel.put(in, file.getFileName().toString());
                }
                Files.move(file, partnerOutbox.resolve("sent").resolve(file.getFileName()));
            }
        } catch (JSchException | SftpException | IOException e) {
            log.warn("Settlement upload for partner {} failed: {}", profile.getPartnerId(), e.toString());
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private static List<Path> listFiles(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    @PreDestroy
    public void shutdown() {
        uploadPool.shutdown();
    }
}
