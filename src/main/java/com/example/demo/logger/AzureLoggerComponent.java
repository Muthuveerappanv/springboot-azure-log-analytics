package com.example.demo.logger;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AzureLoggerComponent {

	public static final String SPL_CHAR_REGEX = "[-+.^:,]";
	public static final String UNDERSCORE = "_";
	public static final String AZURE_LA_URL = "https://%s.ods.opinsights.azure.com/api/logs?api-version=2016-04-01";

	public AzureLoggerComponent() {
	}

	public AzureLoggerComponent(LogStoreEnv logStoreEnv) {
		this.logStoreEnv = logStoreEnv;
	}

	@Autowired
	private LogStoreEnv logStoreEnv;

	@Value("${spring.application.name}")
	private String appName;

	@Value("${spring.application.version}")
	private String appVersion;

	private RestTemplate restTemplate = new RestTemplate();

	@Async
	public void pushLogsToAzure(String json) {
		if (logStoreEnv == null || StringUtils.isEmpty(logStoreEnv.getAzureWid())
				|| StringUtils.isEmpty(logStoreEnv.getAzureSharedKey())) {
			return;
		}
		try {
			UUID.fromString(logStoreEnv.getAzureWid());
		} catch (NullPointerException | IllegalArgumentException e) {
			return;
		}
		logStoreEnv.setAppName(appName);
		logStoreEnv.setAppVersion(appVersion);
		RequestEntity<String> azureLogRqst = constructHttpEntity(json, false);
		// RestTemplate restTemplate = new RestTemplate();
		restTemplate.exchange(azureLogRqst, String.class);
	}

	public void pushSysLogsToAzure(String json) {
		if (logStoreEnv == null || StringUtils.isEmpty(logStoreEnv.getAzureWid())
				|| StringUtils.isEmpty(logStoreEnv.getAzureSharedKey())) {
			return;
		}
		try {
			UUID.fromString(logStoreEnv.getAzureWid());
		} catch (NullPointerException | IllegalArgumentException e) {
			return;
		}
		// execute in async thread
		new Thread(() -> {
			RequestEntity<String> azureLogRqst = constructHttpEntity(json, true);
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.exchange(azureLogRqst, String.class);
		}).start();
	}

	private RequestEntity<String> constructHttpEntity(String json, Boolean isSysLog) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ssZ");
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());

		SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		String date = fmt.format(Calendar.getInstance().getTime()) + " GMT";

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("Log-Type",
				constructAzureLogNm(logStoreEnv.getAppName(), logStoreEnv.getAppVersion(), isSysLog));
		httpHeaders.add("x-ms-date", date);
		json = StringUtils.stripAccents(json);
		httpHeaders.add("Authorization", computeAuthHdr(json, isSysLog, date));
		httpHeaders.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		httpHeaders.set("time-generated-field", nowAsISO);

		String azueLogUrl = String.format(AZURE_LA_URL, logStoreEnv.getAzureWid());

		return new RequestEntity<String>(json, httpHeaders, HttpMethod.POST,
				UriComponentsBuilder.fromHttpUrl(azueLogUrl).build().toUri());
	}

	private String computeAuthHdr(String jsonBody, Boolean isSysLog, String date) {
		Integer bodyLength = jsonBody.length();
		if (!isSysLog) {
			bodyLength = jsonBody.getBytes(StandardCharsets.UTF_8).length;
		}
		String signString = "POST\n" + bodyLength + "\n" + MediaType.APPLICATION_JSON_VALUE + "\n" + "x-ms-date:" + date
				+ "\n" + "/api/logs";
		return createAuthorizationHeader(signString);
	}

	private String createAuthorizationHeader(String canonicalizedString) {
		String authStr = null;
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(Base64.getDecoder().decode(logStoreEnv.getAzureSharedKey()), "HmacSHA256"));
			String authKey = new String(Base64.getEncoder().encode(mac.doFinal(canonicalizedString.getBytes("UTF-8"))));
			authStr = "SharedKey " + logStoreEnv.getAzureWid() + ":" + authKey;
		} catch (Exception e) {
			Logger logger = LoggerFactory.getLogger(this.getClass());
			logger.error("Error while sending message to Log Analytics", e);

		}
		return authStr;
	}

	public static String constructAzureLogNm(String appName, String appVersion, Boolean isSysLog) {
		String logTypeInd = null;
		String filteredAppNm = StringUtils.replaceAll(appName, SPL_CHAR_REGEX, "");
		if (StringUtils.isEmpty(appName)) {
			appName = "default";
		}
		if (StringUtils.isEmpty(appVersion)) {
			appVersion = "v0";
		}
		if (isSysLog) {
			// syslogs - rqst/resp/exceptions
			logTypeInd = "sl";
		} else {
			// applicatoin logs written per operation
			logTypeInd = "al";
		}
		return StringUtils.join(Arrays.asList(filteredAppNm, appVersion, logTypeInd), UNDERSCORE);
	}
}
