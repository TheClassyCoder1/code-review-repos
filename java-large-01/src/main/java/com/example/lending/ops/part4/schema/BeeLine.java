package com.example.lending.ops.part4.schema;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/** Minimal state of the reporting SQL shell: user options and console output. */
public class BeeLine {

  public static class BeeLineOpts extends Properties {
    private final File propertiesFile;

    public BeeLineOpts(File propertiesFile) { this.propertiesFile = propertiesFile; }

    public File getPropertiesFile() { return propertiesFile; }

    public void save() throws IOException {
      try (OutputStream out = new FileOutputStream(propertiesFile)) {
        store(out, "reporting shell preferences");
      }
    }
  }

  private final BeeLineOpts opts;

  public BeeLine(BeeLineOpts opts) { this.opts = opts; }

  public BeeLineOpts getOpts() { return opts; }

  public void info(String message) { System.out.println(message); }

  public String loc(String key, Object arg) { return key + ": " + arg; }
}
