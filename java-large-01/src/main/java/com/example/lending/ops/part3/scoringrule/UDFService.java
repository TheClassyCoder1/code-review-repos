package com.example.lending.ops.part3.scoringrule;

public interface UDFService {
  String downLoadUDF(long udfId, String version, String user) throws Exception;
}
