package com.example.lending.ops.part3.workflow;

import com.example.lending.ops.common.StringUtils;
import java.util.Properties;

/** Resolves which underwriting workflow node a job runs under from its job properties. */
public final class ContextServiceUtils {
  static final class CSCommonUtils { static final String CONTEXT_ID_STR = "contextID", NODE_NAME_STR = "nodeName", NODE_ID_SEPARATOR = "@@"; }
  private ContextServiceUtils() { }
  /** Combined ids look like {@code <contextId>@@<nodeName>}; ids without a node part map to an empty name. */
  static String getNodeNameByCombinedNodeIDContextID(String combinedId) {
    int idx = combinedId.lastIndexOf(CSCommonUtils.NODE_ID_SEPARATOR);
    return idx < 0 ? "" : combinedId.substring(idx + CSCommonUtils.NODE_ID_SEPARATOR.length());
  }
  public static String getNodeNameStrByProperties(Properties properties) {
    Object contextIDValue = properties.get(CSCommonUtils.CONTEXT_ID_STR);
    if (null == contextIDValue) {
      return null;
    }
    String nodeName = null;

    if (null != properties) {
      Object value = properties.get(CSCommonUtils.NODE_NAME_STR);
      if (null != value) {
        nodeName = value.toString();
      }
    }
    if (StringUtils.isBlank(nodeName)) {
      nodeName = getNodeNameByCombinedNodeIDContextID(contextIDValue.toString());
    }
    properties.put(CSCommonUtils.NODE_NAME_STR, nodeName);
    return nodeName;
  }
}
