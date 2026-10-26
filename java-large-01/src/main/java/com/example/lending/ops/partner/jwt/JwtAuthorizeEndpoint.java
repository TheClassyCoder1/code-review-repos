package com.example.lending.ops.partner.jwt;

import com.example.lending.ops.common.StringUtils;
import com.example.lending.ops.common.apidoc.ApiOperation.Operation;
import com.example.lending.ops.partner.jwt.JwtDetailsService.AppsJwtDetails;
import com.example.lending.ops.partner.jwt.JwtLoginService.WebContext;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/** Public JWK metadata for partner apps that verify ops-console login tokens. */
@Controller
@RequiredArgsConstructor
public class JwtAuthorizeEndpoint {
    interface WebConstants { String MXK_METADATA_PREFIX = "mxk_metadata_"; }

    interface ContentType { String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8", APPLICATION_XML_UTF8 = "application/xml;charset=UTF-8"; }

    record JWKSetKeyStore(String json) {
        String toString(String mediaType) { return "xml".equalsIgnoreCase(mediaType) ? "<jwks><![CDATA[" + json + "]]></jwks>" : json; }
    }

    private final JwtDetailsService jwtDetailsService;

    @Operation(summary = "JWT JWK元数据接口", description = "参数mxk_metadata_APPID")
    @GetMapping(value = "/metadata/jwt/" + WebConstants.MXK_METADATA_PREFIX + "{appid}.{mediaType}")
    @ResponseBody
    public String  metadata(HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable("appid") String appId,
            @PathVariable String mediaType) {
        AppsJwtDetails jwtDetails = jwtDetailsService.get(appId , true);
        if(jwtDetails != null) {
            String jwkSetString = "";
            if(!"none".equalsIgnoreCase(jwtDetails.getSignature())) {
                jwkSetString = jwtDetails.getSignatureKey();
            }
            if(!"none".equalsIgnoreCase(jwtDetails.getAlgorithm())) {
                if(StringUtils.isBlank(jwkSetString)) {
                    jwkSetString = jwtDetails.getAlgorithmKey();
                }else {
                    jwkSetString = jwkSetString + "," +jwtDetails.getAlgorithmKey();
                }
            }

            JWKSetKeyStore jwkSetKeyStore = new JWKSetKeyStore("{\"keys\": [" + jwkSetString + "]}");
            if(StringUtils.isNotBlank(mediaType)
                    && "xml".equalsIgnoreCase(mediaType)) {
                response.setContentType(ContentType.APPLICATION_XML_UTF8);
            }else {
                response.setContentType(ContentType.APPLICATION_JSON_UTF8);
            }
            return jwkSetKeyStore.toString(mediaType);

        }
        return appId + " not exist. \n" + WebContext.version();
    }
}
