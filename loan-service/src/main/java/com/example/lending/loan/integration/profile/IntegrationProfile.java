package com.example.lending.loan.integration.profile;

import java.util.LinkedHashMap;
import java.util.Map;

/** Connection profile of an external servicing integration (bureau, payments, messaging). */
public class IntegrationProfile {

    private String id;
    private String rev;
    private String name;
    private String selectedEndpointUrl;
    private boolean running;
    private Map<String, String> secrets = new LinkedHashMap<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRev() { return rev; }
    public void setRev(String rev) { this.rev = rev; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSelectedEndpointUrl() { return selectedEndpointUrl; }
    public void setSelectedEndpointUrl(String selectedEndpointUrl) { this.selectedEndpointUrl = selectedEndpointUrl; }
    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
    public Map<String, String> getSecrets() { return secrets; }
    public void setSecrets(Map<String, String> secrets) { this.secrets = secrets; }
}
