package com.example.lending.loan.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ops/notification-channels")
public class NotificationChannelController {

    public static final String CHANNELS_KEY = "notification.channels";
    public static final TypeReference<List<NotificationChannel>> CHANNEL_LIST = new TypeReference<>() { };

    private final SettingStore settingStore;
    private final ObjectMapper objectMapper;
    private final String gatewayHost;

    public NotificationChannelController(SettingStore settingStore, ObjectMapper objectMapper,
                                         @Value("${notifications.gateway.tls-host}") String gatewayHost) {
        this.settingStore = settingStore;
        this.objectMapper = objectMapper;
        this.gatewayHost = gatewayHost;
    }

    @GetMapping("/gateway-certificate")
    public Map<String, String> gatewayCertificate() throws Exception {
        byte[] encoded = GatewayCertificates.getServerCertificate(gatewayHost, 443).getEncoded();
        return Map.of("host", gatewayHost,
                "sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encoded)));
    }

    @GetMapping
    public String list() {
        String channels = settingStore.get(CHANNELS_KEY);
        return channels == null ? "[]" : channels;
    }

    @PutMapping("/edit")
    public ResponseEntity<String> updateById(@RequestBody NotificationChannel channel) throws JsonProcessingException {
        String channelList = settingStore.get(CHANNELS_KEY);
        if (null != channelList) {
            List<NotificationChannel> channels = objectMapper.readValue(channelList, CHANNEL_LIST);
            channels.forEach(item -> {
                if (item.getId().equals(channel.getId())) {
                    BeanUtils.copyProperties(channel, item);
                }
            });
            settingStore.set(CHANNELS_KEY, objectMapper.writeValueAsString(channels));
        } else {
            return ResponseEntity.ok("Edit failed, channel not found");
        }
        return ResponseEntity.ok("Edit succeeded");
    }
}
