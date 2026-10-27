package com.example.lending.ops.partner;

import com.example.lending.ops.common.Result.Message;
import com.example.lending.ops.partner.Apps.AppsExtendApiDetails;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/** Partner apps integrated through the extend-API protocol (partner mail, HR portals). */
@RestController
@RequestMapping("/apps/extendapi")
public class ExtendApiDetailsController extends BaseAppController {
    public ExtendApiDetailsController(AppsService appsService, @Value("${ops.partner.secret-key}") String key,
                                      @Value("${ops.partner.secret-salt}") String salt) {
        super(appsService, key, salt);
    }

    @GetMapping(value = { "/get/{id}" }, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<AppsExtendApiDetails> get(@PathVariable String id) {
        Apps application= appsService.get(id);
        super.decoderSecret(application);
        AppsExtendApiDetails extendApiDetails=new AppsExtendApiDetails();
        BeanUtils.copyProperties(application, extendApiDetails);
        extendApiDetails.transIconBase64();
        return new Message<>(extendApiDetails);
    }
}
