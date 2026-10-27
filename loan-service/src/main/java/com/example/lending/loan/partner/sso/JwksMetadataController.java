package com.example.lending.loan.partner.sso;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksMetadataController {

    private static final String VERSION = "lending-partner-sso/1.0";

    private final PartnerJwtSettingsRepository jwtDetailsService;

    public JwksMetadataController(PartnerJwtSettingsRepository jwtDetailsService) {
        this.jwtDetailsService = jwtDetailsService;
    }

    @GetMapping(value = "/partner/jwks/metadata/{appid}.{mediaType}")
    public String metadata(HttpServletResponse response,
            @PathVariable("appid") String appId,
            @PathVariable String mediaType) {
        PartnerJwtSettings jwtDetails = jwtDetailsService.findById(appId).orElse(null);
        if(jwtDetails != null) {
            String jwkSetString = "";
            if(!"none".equalsIgnoreCase(jwtDetails.getSignature())) {
                jwkSetString = jwtDetails.getSignatureKey();
            }
            if(!"none".equalsIgnoreCase(jwtDetails.getAlgorithm())) {
                if(!StringUtils.hasText(jwkSetString)) {
                    jwkSetString = jwtDetails.getAlgorithmKey();
                }else {
                    jwkSetString = jwkSetString + "," +jwtDetails.getAlgorithmKey();
                }
            }

            JwkSetDocument jwkSetKeyStore = new JwkSetDocument("{\"keys\": [" + jwkSetString + "]}");
            if(StringUtils.hasText(mediaType)
                    && "xml".equalsIgnoreCase(mediaType)) {
                response.setContentType(MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8");
            }else {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            }
            return jwkSetKeyStore.toString(mediaType);

        }
        return appId + " not exist. \n" + VERSION;
    }
}
