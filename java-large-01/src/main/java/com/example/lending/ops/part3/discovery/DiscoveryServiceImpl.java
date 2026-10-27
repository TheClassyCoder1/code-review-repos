package com.example.lending.ops.part3.discovery;

import com.example.lending.ops.part3.discovery.DiscoveryMappers.*;
import com.example.lending.ops.part3.discovery.DiscoveryModel.*;
import com.example.lending.ops.part3.discovery.DiscoveryProcessorHolder.DiscoveryProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.sql.Timestamp;
import java.util.*;

/** Binds partner upstream selectors to a discovery source and activates the watcher once the binding commits. */
@Service
public class DiscoveryServiceImpl {
    private final DiscoveryMapper discoveryMapper;
    private final DiscoveryHandlerMapper discoveryHandlerMapper;
    private final DiscoveryRelMapper discoveryRelMapper;
    private final DiscoveryProcessorHolder discoveryProcessorHolder;
    private final TransactionTemplate discoveryActivation;
    public DiscoveryServiceImpl(DiscoveryMapper discoveryMapper, DiscoveryHandlerMapper discoveryHandlerMapper,
            DiscoveryRelMapper discoveryRelMapper, DiscoveryProcessorHolder discoveryProcessorHolder, PlatformTransactionManager tm) {
        this.discoveryMapper = discoveryMapper;
        this.discoveryHandlerMapper = discoveryHandlerMapper;
        this.discoveryRelMapper = discoveryRelMapper;
        this.discoveryProcessorHolder = discoveryProcessorHolder;
        this.discoveryActivation = new TransactionTemplate(tm);
        this.discoveryActivation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    @Transactional
    public void registerDiscoveryConfig(DiscoveryConfigRegisterDTO dto, SelectorDO selector) { bindingDiscovery(dto, selector); }
    private void bindingDiscovery(final DiscoveryConfigRegisterDTO discoveryConfigRegisterDTO, final SelectorDO selectorDO) {
        ProxySelectorDTO proxySelectorDTO = new ProxySelectorDTO();
        proxySelectorDTO.setName(selectorDO.getSelectorName());
        proxySelectorDTO.setId(selectorDO.getId());
        proxySelectorDTO.setPluginName(discoveryConfigRegisterDTO.getPluginName());
        proxySelectorDTO.setNamespaceId(selectorDO.getNamespaceId());
        DiscoveryDO discoveryDO = discoveryMapper.selectByPluginNameAndLevelAndNamespaceIdAndType(discoveryConfigRegisterDTO.getPluginName(),
                DiscoveryLevel.PLUGIN.getCode(), discoveryConfigRegisterDTO.getNamespaceId(), discoveryConfigRegisterDTO.getDiscoveryType());
        if (Objects.isNull(discoveryDO)) {
            Timestamp currentTime = new Timestamp(System.currentTimeMillis());
            discoveryDO = DiscoveryDO.builder()
                    .id(UUIDUtils.getInstance().generateShortUuid())
                    .discoveryName(discoveryConfigRegisterDTO.getName())
                    .pluginName(discoveryConfigRegisterDTO.getPluginName())
                    .discoveryType(discoveryConfigRegisterDTO.getDiscoveryType())
                    .discoveryLevel(DiscoveryLevel.PLUGIN.getCode())
                    .serverList(discoveryConfigRegisterDTO.getServerList())
                    .props(GsonUtils.getInstance().toJson(Optional.ofNullable(discoveryConfigRegisterDTO.getProps()).orElse(new Properties())))
                    .namespaceId(discoveryConfigRegisterDTO.getNamespaceId())
                    .dateCreated(currentTime)
                    .dateUpdated(currentTime)
                    .build();
            discoveryMapper.insertSelective(discoveryDO);
        }
        DiscoveryHandlerDO discoveryHandlerDO = discoveryHandlerMapper.selectBySelectorId(selectorDO.getId());
        if (Objects.isNull(discoveryHandlerDO)) {
            discoveryHandlerDO = DiscoveryHandlerDO.builder()
                    .id(UUIDUtils.getInstance().generateShortUuid())
                    .discoveryId(discoveryDO.getId())
                    .handler(discoveryConfigRegisterDTO.getHandler())
                    .listenerNode(discoveryConfigRegisterDTO.getListenerNode())
                    .props(GsonUtils.getInstance().toJson(Optional.ofNullable(discoveryConfigRegisterDTO.getProps()).orElse(new Properties())))
                    .dateCreated(new Timestamp(System.currentTimeMillis())).build();
            DiscoveryRelDO discoveryRefDO = DiscoveryRelDO.builder()
                    .id(UUIDUtils.getInstance().generateShortUuid())
                    .discoveryHandlerId(discoveryHandlerDO.getId())
                    .selectorId(selectorDO.getId())
                    .pluginName(discoveryConfigRegisterDTO.getPluginName()).build();
            discoveryRelMapper.insertSelective(discoveryRefDO);
            discoveryHandlerMapper.insertSelective(discoveryHandlerDO);
        }
        DiscoveryProcessor discoveryProcessor = discoveryProcessorHolder.chooseProcessor(discoveryConfigRegisterDTO.getDiscoveryType());
        final DiscoveryDO registeredDiscovery = discoveryDO;
        final DiscoveryHandlerDO registeredHandler = discoveryHandlerDO;
        Runnable activate = () -> {
            discoveryProcessor.createDiscovery(registeredDiscovery);
            discoveryProcessor.createProxySelector(DiscoveryTransfer.INSTANCE.mapToDTO(registeredHandler), proxySelectorDTO);
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            activate.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Watchers may synchronously persist upstreams; suspend the already committed transaction resources.
                discoveryActivation.executeWithoutResult(status -> activate.run());
            }
        });
    }
}
