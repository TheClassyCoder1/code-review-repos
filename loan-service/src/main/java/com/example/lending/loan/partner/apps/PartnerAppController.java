package com.example.lending.loan.partner.apps;

import com.example.lending.loan.partner.account.PartnerSession;
import com.example.lending.loan.support.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/partner/apps")
public class PartnerAppController {

    private final PartnerAppRepository appsService;
    private final AppOperationLogService applicationLogService;
    private final SecretCodec secretCodec;

    public PartnerAppController(PartnerAppRepository appsService,
                                AppOperationLogService applicationLogService,
                                SecretCodec secretCodec) {
        this.appsService = appsService;
        this.applicationLogService = applicationLogService;
        this.secretCodec = secretCodec;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<List<PartnerAppDetails>> list(HttpServletRequest request) {
        List<PartnerAppDetails> apps = appsService.findByTenantId(PartnerSession.tenantId(request)).stream()
                .map(app -> {
                    PartnerAppDetails details = new PartnerAppDetails();
                    BeanUtils.copyProperties(app, details, "clientSecret");
                    details.transIconBase64();
                    return details;
                })
                .toList();
        return ApiResponse.ofSuccess(apps);
    }

    @GetMapping(value = { "/get/{id}" }, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ApiResponse<PartnerAppDetails> get(@PathVariable String id) {
        PartnerApp application= appsService.findById(id).orElseThrow();
        secretCodec.decoderSecret(application);
        PartnerAppDetails extendApiDetails=new PartnerAppDetails();
        BeanUtils.copyProperties(application, extendApiDetails);
        extendApiDetails.transIconBase64();
        return ApiResponse.ofSuccess(extendApiDetails);
    }

    @GetMapping("/{id}/opt_logs")
    public ApiResponse<List<AppOperationLog>> operationLogs(HttpServletRequest request, @PathVariable String id) {
        return ApiResponse.ofSuccess(applicationLogService.list(id, PartnerSession.tenantId(request)));
    }

    @PostMapping("delete/opt_log")
    public ApiResponse<Boolean> deleteOperationLog(@RequestBody IdRequest request) {
        Boolean deleted = applicationLogService.removeById(request.getId());
        return ApiResponse.ofSuccess(deleted);
    }
}
