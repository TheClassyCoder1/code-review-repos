package com.example.lending.ops.part4.schema;

/** One column change in a reporting-table schema migration (add / drop / rename / retype). */
public class UpdateColumn implements java.io.Serializable {

  public java.lang.String name;
  public java.lang.String operate;
  public java.lang.Boolean isOptional;

  public UpdateColumn() {
    this.isOptional = Boolean.TRUE;
  }

  public UpdateColumn(
    java.lang.String name,
    java.lang.String operate)
  {
    this();
    this.name = name;
    this.operate = operate;
  }
}
