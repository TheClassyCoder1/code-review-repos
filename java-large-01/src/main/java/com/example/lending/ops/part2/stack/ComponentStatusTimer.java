package com.example.lending.ops.part2.stack;

import com.example.lending.ops.part2.stack.StackDao.*;
import com.example.lending.ops.part2.stack.StackModel.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Polls every worker-host component for liveness and rolls the result up to its service. */
@Component
@RequiredArgsConstructor
public class ComponentStatusTimer {

    private final ComponentDao componentDao;
    private final HostDao hostDao;
    private final ServiceDao serviceDao;

    @Async
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void execute() {
        List<ComponentPO> componentPOList = componentDao.findAll();
        for (ComponentPO componentPO : componentPOList) {
            ComponentDTO componentDTO = StackUtils.getComponentDTO(componentPO.getName());
            String category = componentDTO.getCategory();
            if (HealthyStatusEnum.fromCode(componentPO.getStatus()) == HealthyStatusEnum.UNKNOWN
                    || category.equals(ComponentCategories.CLIENT)) {
                continue;
            }

            ComponentPO componentDetailsPO = componentDao.findDetailsById(componentPO.getId());
            HostPO hostPO = hostDao.findById(componentPO.getHostId());
            ComponentStatusRequest request = ComponentStatusRequest.newBuilder()
                    .setStackName(
                            CaseUtils.toLowerCase(componentDetailsPO.getStack().split("-")[0]))
                    .setStackVersion(componentDetailsPO.getStack().split("-")[1])
                    .setServiceName(componentDetailsPO.getServiceName())
                    .setServiceUser(componentDetailsPO.getServiceUser())
                    .setComponentName(componentDetailsPO.getName())
                    .build();
            ComponentStatusServiceGrpc.ComponentStatusServiceBlockingStub blockingStub = GrpcClient.getBlockingStub(
                    hostPO.getHostname(),
                    hostPO.getGrpcPort(),
                    ComponentStatusServiceGrpc.ComponentStatusServiceBlockingStub.class);
            ComponentStatusReply reply = blockingStub.getComponentStatus(request);

            // Status 0 means the service is running
            if (reply.getStatus() == 0) {
                componentPO.setStatus(HealthyStatusEnum.HEALTHY.getCode());
            } else {
                componentPO.setStatus(HealthyStatusEnum.UNHEALTHY.getCode());
            }
        }

        componentDao.partialUpdateByIds(componentPOList);

        // Update services
        Map<Long, List<ComponentPO>> componentPOMap =
                componentPOList.stream().collect(Collectors.groupingBy(ComponentPO::getServiceId));
        for (Map.Entry<Long, List<ComponentPO>> entry : componentPOMap.entrySet()) {
            Long serviceId = entry.getKey();
            List<ComponentPO> components = entry.getValue();
            ServicePO servicePO = serviceDao.findById(serviceId);
            boolean hasUnknownComponent = components.stream()
                    .anyMatch(component -> Objects.equals(component.getStatus(), HealthyStatusEnum.UNKNOWN.getCode()));
            if (hasUnknownComponent) {
                continue;
            }

            List<ComponentPO> healthyComponents = components.stream()
                    .filter(component -> Objects.equals(component.getStatus(), HealthyStatusEnum.HEALTHY.getCode()))
                    .toList();

            if (healthyComponents.size() == components.size()) {
                servicePO.setStatus(HealthyStatusEnum.HEALTHY.getCode());
                servicePO.setRestartFlag(false);
            } else {
                servicePO.setStatus(HealthyStatusEnum.UNHEALTHY.getCode());
                servicePO.setRestartFlag(true);
            }

            serviceDao.partialUpdateById(servicePO);
        }
    }
}
