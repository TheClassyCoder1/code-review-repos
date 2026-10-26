package com.example.lending.ops.part4.schema;

import java.io.IOException;

/** Shell meta-commands of the reporting SQL shell. */
public class Commands {

  private final BeeLine beeLine;

  public Commands(BeeLine beeLine) {
    this.beeLine = beeLine;
  }

  public boolean save(String line) throws IOException {
    beeLine.info(beeLine.loc("saving-options", beeLine.getOpts().getPropertiesFile()));
    beeLine.getOpts().save();
    return true;
  }
}
