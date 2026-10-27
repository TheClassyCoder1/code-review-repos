package com.example.lending.ops.part3.scoringrule;

import com.example.lending.ops.part3.scoringrule.BmlClient.BmlDownloadResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;

/** Scoring rules are functions risk analysts upload; script rules can be viewed in the console. */
@Service
@RequiredArgsConstructor
public class UDFServiceImpl implements UDFService {
  private static final Logger logger = LoggerFactory.getLogger(UDFServiceImpl.class);
  private static final int UDF_JAR = 0;
  private final UDFDao udfDao;
  private final UDFDao udfVersionDao;
  private final BmlClient bmlClient;
  private BmlDownloadResponse downloadBml(String user, String resourceId, String version) { return bmlClient.download(user, resourceId, version); }
  @Override
  public String downLoadUDF(long udfId, String version, String user) throws Exception {
    logger.info("user " + user + " begin to downLoad udf, udfId: " + udfId);
    UDFInfo udfInfo = udfDao.getUDFById(udfId);
    if (udfInfo.getUdfType() == UDF_JAR) {
      throw new UDFException("jar类型的udf不支持下载查看内容");
    }
    UDFVersion udfVersion = udfVersionDao.selectByUdfIdAndVersion(udfId, version);
    BmlDownloadResponse downloadResponse =
        downloadBml(user, udfVersion.getBmlResourceId(), udfVersion.getBmlResourceVersion());
    String content =
        IOUtils.toString(downloadResponse.inputStream(), Configuration.BDP_ENCODING().getValue());
    IOUtils.closeQuietly(downloadResponse.inputStream());
    logger.info("user " + user + " end to downLoad udf, udfId: " + udfId);
    return content;
  }
  record UDFInfo(int getUdfType) { }
  record UDFVersion(String getBmlResourceId, String getBmlResourceVersion) { }
  public static class UDFException extends Exception { UDFException(String message) { super(message); } }
  static final class Configuration { record Setting(String getValue) { } static Setting BDP_ENCODING() { return new Setting("UTF-8"); } }
  static final class IOUtils {
    static String toString(InputStream in, String encoding) throws IOException { return new String(in.readAllBytes(), Charset.forName(encoding)); }
    static void closeQuietly(InputStream in) { try { in.close(); } catch (IOException ignored) { /* nothing to release */ } }
  }
  @Repository
  @RequiredArgsConstructor
  static class UDFDao {
    private final NamedParameterJdbcTemplate jdbc;
    UDFInfo getUDFById(long id) {
      return jdbc.queryForObject("SELECT udf_type FROM scoring_udf WHERE id = :id", Map.of("id", id), (rs, i) -> new UDFInfo(rs.getInt(1)));
    }
    UDFVersion selectByUdfIdAndVersion(long id, String version) {
      return jdbc.queryForObject("SELECT bml_resource_id, bml_resource_version FROM scoring_udf_version WHERE udf_id = :id AND version = :v",
          Map.of("id", id, "v", version), (rs, i) -> new UDFVersion(rs.getString(1), rs.getString(2)));
    }
  }
}
