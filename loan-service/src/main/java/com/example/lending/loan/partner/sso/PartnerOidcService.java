package com.example.lending.loan.partner.sso;

import com.example.lending.loan.partner.account.PartnerSession;
import com.example.lending.loan.partner.account.PartnerUser;
import com.example.lending.loan.partner.account.PartnerUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PartnerOidcService {

    private final OidcProperties properties;
    private final PartnerUserRepository partnerUserRepository;
    private final RestTemplate restTemplate;

    public PartnerOidcService(OidcProperties properties, PartnerUserRepository partnerUserRepository,
                              RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.partnerUserRepository = partnerUserRepository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    public URI createAuthUri() {
        return UriComponentsBuilder.fromUri(properties.getAuthUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getCallbackUrl())
                .queryParam("scope", "openid profile email groups")
                .encode()
                .build()
                .toUri();
    }

    @Transactional
    public URI handleCallback(String queryParameters, HttpServletRequest request) throws GeneralSecurityException {

        String redirectUriOverride = request.getParameter("redirect_uri");
        URI redirectUri;
        if (redirectUriOverride != null) {
            redirectUri = URI.create(redirectUriOverride);
            if (!"lending.partner.app".equals(redirectUri.getScheme())) {
                throw new GeneralSecurityException("Invalid redirect URI");
            }
        } else {
            redirectUri = properties.getCallbackUrl();
        }
        Map<String, String> response = UriComponentsBuilder.newInstance().query(queryParameters).build()
                .getQueryParams().toSingleValueMap();

        if (response.containsKey("error")) {
            throw new GeneralSecurityException(response.getOrDefault("error_description", response.get("error")));
        }

        String authCode = response.get("code");
        if (authCode == null) {
            throw new GeneralSecurityException("Malformed OpenID callback");
        }

        String bearerToken = getToken(redirectUri, authCode);

        JsonNode userInfo = getUserInfo(bearerToken);

        List<String> userGroups = groups(userInfo.path(properties.getGroupsClaimName()));
        String adminGroup = properties.getAdminGroup();
        String allowGroup = properties.getAllowGroup();
        Boolean administrator = adminGroup != null && userGroups != null ? userGroups.contains(adminGroup) : null;

        if (!(Boolean.TRUE.equals(administrator) || allowGroup == null
                || (userGroups != null && userGroups.contains(allowGroup)))) {
            throw new GeneralSecurityException("Your OpenID Groups do not permit access");
        }

        PartnerUser user = login(userInfo.path("email").asText(), userInfo.path("name").asText(), administrator);

        PartnerSession.userLogin(request, user);

        return properties.getBaseUrl().resolve("?openid=success");
    }

    private String getToken(URI redirectUri, String authCode) throws GeneralSecurityException {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", authCode);
        form.add("redirect_uri", redirectUri.toString());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(properties.getClientId(), properties.getClientSecret());
        JsonNode tokens = restTemplate.postForObject(properties.getTokenUrl(), new HttpEntity<>(form, headers), JsonNode.class);
        if (tokens == null || !tokens.hasNonNull("access_token")) {
            throw new GeneralSecurityException("Token endpoint returned no access token");
        }
        return tokens.get("access_token").asText();
    }

    private JsonNode getUserInfo(String bearerToken) throws GeneralSecurityException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        JsonNode userInfo = restTemplate.exchange(properties.getUserInfoUrl(), HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class).getBody();
        if (userInfo == null) {
            throw new GeneralSecurityException("UserInfo endpoint returned no body");
        }
        return userInfo;
    }

    private static List<String> groups(JsonNode node) {
        if (!node.isArray()) {
            return null;
        }
        List<String> groups = new ArrayList<>();
        node.forEach(group -> groups.add(group.asText()));
        return groups;
    }

    private PartnerUser login(String email, String name, Boolean administrator) throws GeneralSecurityException {
        PartnerUser user = partnerUserRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralSecurityException("No partner account is registered for this identity"));
        user.setDisplayName(name);
        if (administrator != null) {
            user.setAdministrator(administrator);
        }
        return user;
    }
}
