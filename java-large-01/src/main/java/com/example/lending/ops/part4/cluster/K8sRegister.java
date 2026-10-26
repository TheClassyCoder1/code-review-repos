package com.example.lending.ops.part4.cluster;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;

/** Labels this pod in the cluster so batch workers can discover the ops console. */
@Component
public class K8sRegister {

    static final String KUBE_TOKEN_FILE = "/var/run/secrets/kubernetes.io/serviceaccount/token";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String podUrl;

    public K8sRegister(@Value("${ops.k8s.pod-url:https://kubernetes.default.svc/api/v1/namespaces/lending-ops/pods/ops-console}") String podUrl) {
        this.podUrl = podUrl;
    }

    public void register(String serviceName) throws Exception {
        KubeTokenPreflight.verify(KUBE_TOKEN_FILE);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getKubeToken());
        Map<String, Object> patch = Map.of("metadata", Map.of("labels", Map.of("lending.example.com/service", serviceName)));
        restTemplate.exchange(podUrl, HttpMethod.PATCH, new HttpEntity<>(patch, headers), String.class);
    }

    private String getKubeToken() throws Exception {
        File file = new File(KUBE_TOKEN_FILE);
        if (file.canRead()) {
            FileReader reader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(reader);
            try {
                String token = bufferedReader.readLine();
                token = token.trim();
                return token;
            } finally {
                bufferedReader.close();
            }
        }
        throw new HugeException("Kubernetes token file doesn't exist");

    }
}
