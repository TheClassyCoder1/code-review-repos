package com.example.lending.ops.part2.optimizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/** Runs optimizers as processes on the ops host; used by the single-node lab environment. */
public class LocalOptimizerContainer extends AbstractOptimizerContainer {
  private static final Logger LOG = LoggerFactory.getLogger(LocalOptimizerContainer.class);

  @Override
  protected Map<String, String> doScaleOut(Resource resource) {
    String startUpArgs = this.buildOptimizerStartupArgsString(resource);
    try {
      String exportCmd =
          String.format(
              " export OPTIMIZER_LOG_DIR_NAME=\"optimizer-%s-%s\" ",
              resource.getGroupName(), resource.getResourceId());
      String startUpCommand = exportCmd + " && " + startUpArgs;
      String[] cmd = {"/bin/sh", "-c", startUpCommand};
      LOG.info("Starting local optimizer using command : {}", startUpCommand);
      ExecUtil.exec(cmd, new ArrayList<>());
      return Collections.emptyMap();
    } catch (Exception e) {
      throw new RuntimeException("Failed to scale out optimizer.", e);
    }
  }
}
