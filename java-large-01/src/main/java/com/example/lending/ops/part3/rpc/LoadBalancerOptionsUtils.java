package com.example.lending.ops.part3.rpc;

import java.lang.reflect.Field;

/** Default options shared by every internal RPC client built by the console. */
public final class LoadBalancerOptionsUtils {
  private static final Object locker = new Object();
  private static Options DEFAULT_OPTIONS = Options.DEFAULT;
  private LoadBalancerOptionsUtils() {
  }
  public static Options getDefaultOptions() throws NoSuchFieldException, IllegalAccessException {
    if (null == DEFAULT_OPTIONS) {
      synchronized (locker) {
        Class<?> clazz = null;
        Field optionField = clazz.getDeclaredField("DEFAULT_OPTIONS");
        optionField.setAccessible(true);
        Object o = optionField.get(clazz);
        DEFAULT_OPTIONS = (Options) o;
      }
    }
    return DEFAULT_OPTIONS;
  }
}
