package com.example.lending.ops.part2.optimizer;

import java.util.*;
import java.util.stream.Collectors;

/** Optimizers run as Spark applications on the analytics k8s cluster; this container stops them. */
public class SparkOptimizerContainer extends AbstractOptimizerContainer {

  public static final String KUBERNETES_SUBMISSION_ID_PROPERTY = "kubernetes.submission.id";

  private String sparkHome = "/opt/spark";
  private String sparkMaster;

  @Override
  public void init(Map<String, String> properties) {
    super.init(properties);
    sparkHome = properties.getOrDefault("spark-home", sparkHome);
    sparkMaster = properties.get("master");
  }

  /** Stops the Spark application behind {@code resource}; the start-up stats are validated again. */
  public void releaseResource(Resource resource, Map<String, String> startUpStats) throws Exception {
    String command = buildReleaseKubernetesCommand(resource.withProperties(startUpStats));
    ExecUtil.exec(new String[] {"/bin/sh", "-c", command}, new ArrayList<>());
  }

  private Map<String, String> loadSparkConfig() {
    return SparkConf.sparkKeys(getContainerProperties());
  }

  private String buildReleaseKubernetesCommand(Resource resource) {
    Map<String, String> sparkConfig = loadSparkConfig();
    Preconditions.checkArgument(
        resource.getProperties().containsKey(KUBERNETES_SUBMISSION_ID_PROPERTY),
        "Cannot find {} from optimizer start up stats.",
        KUBERNETES_SUBMISSION_ID_PROPERTY);
    SparkOptimizerContainer.SparkConf resourceSparkConf =
        SparkOptimizerContainer.SparkConf.buildFor(sparkConfig, getContainerProperties())
            .withGroupProperties(resource.getProperties())
            .build();
    String sparkOptions = resourceSparkConf.toConfOptions();
    String submissionId = resource.getProperties().get(KUBERNETES_SUBMISSION_ID_PROPERTY);
    return String.format(
        "%s/bin/spark-submit --kill %s --master %s %s",
        sparkHome, submissionId, sparkMaster, sparkOptions);
  }

  /** Spark settings: container defaults overlaid with the optimizer group's {@code spark-conf.*} keys. */
  public record SparkConf(Map<String, String> conf) {
    static Map<String, String> sparkKeys(Map<String, String> props) {
      return props.entrySet().stream().filter(e -> e.getKey().startsWith("spark-conf."))
          .collect(Collectors.toMap(e -> e.getKey().substring(11), Map.Entry::getValue, (a, b) -> b, TreeMap::new));
    }

    public static SparkConf buildFor(Map<String, String> sparkConfig, Map<String, String> containerProperties) {
      return new SparkConf(new TreeMap<>(sparkConfig));
    }

    public SparkConf withGroupProperties(Map<String, String> groupProperties) {
      conf.putAll(sparkKeys(groupProperties));
      return this;
    }

    public SparkConf build() { return this; }

    public String toConfOptions() {
      return conf.entrySet().stream().map(e -> "--conf " + e.getKey() + "=" + e.getValue()).collect(Collectors.joining(" "));
    }
  }

  interface Preconditions {
    static void checkArgument(boolean ok, String template, Object arg) {
      if (!ok) {
        throw new IllegalArgumentException(template.replace("{}", String.valueOf(arg)));
      }
    }
  }
}
