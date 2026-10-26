package com.example.lending.ops.part3.letters;

import com.example.lending.ops.part3.support.*;
import com.example.lending.ops.part3.support.HttpRequest.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.util.List;

/** Refreshes borrower letter templates (offer letters, arrears notices) from the shared template repository. */
@Slf4j
@Service
public class LetterTemplateSyncService {
	private final DefaultProperties defaultProperties;
	private final Path templateDir;
	public LetterTemplateSyncService(@Value("${ops.letters.template-branch:main}") String branch,
			@Value("${ops.letters.template-dir:/var/lib/ops-console/letters}") Path templateDir) {
		this.defaultProperties = new DefaultProperties(branch);
		this.templateDir = templateDir;
	}
	public List<String> syncTemplates(List<String> fileNames) throws IOException {
		Files.createDirectories(templateDir);
		for (String fileName : fileNames) {
			if (!fileName.matches("[A-Za-z0-9_-]+\\.(ftl|html)")) {
				throw new CheckedException("非法的模板文件名: " + fileName);
			}
			Files.writeString(templateDir.resolve(fileName), getCGTMFile(fileName));
		}
		return fileNames;
	}
	private String getCGTMFile(String fileName) {
		String requestUrl = String.format("%s/CGTM/raw/%s/%s", DefaultConstants.CGTM_URL, defaultProperties.getBranch(),
				fileName);
		HttpResponse response = HttpRequest.get(requestUrl).execute();
		String responseBody = response.body();
		int httpStatus = response.getStatus();

		if (httpStatus == HttpStatus.HTTP_OK || StrUtil.isNotBlank(responseBody)) {
			return responseBody;
		}

		log.warn("在线更新模板失败:{} ，Http Code:{}", fileName, httpStatus);
		throw new CheckedException("在线更新模板失败，任务终止！");
	}
	static final class DefaultConstants { static final String CGTM_URL = "https://templates.lending.internal"; }
	record DefaultProperties(String getBranch) { }
	public static class CheckedException extends RuntimeException {
		public CheckedException(String message) { super(message); }
	}
}
