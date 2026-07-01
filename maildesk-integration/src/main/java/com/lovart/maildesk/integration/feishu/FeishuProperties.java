package com.lovart.maildesk.integration.feishu;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Feishu app credentials and KOL data source tokens. Values come from env vars
 * (see {@code .env.example} / SETUP.md §4.1).
 */
@ConfigurationProperties(prefix = "maildesk.feishu")
public class FeishuProperties {

    private String appId = "";
    private String appSecret = "";
    /** Spreadsheet token or Bitable app token for the KOL roster workbook. */
    private String kolAppToken = "";
    /** Optional sheet tab id filter, or Bitable table id for legacy imports. */
    private String kolTableId = "";
    /** Override in tests via MockWebServer base URL. */
    private String apiBase = "https://open.feishu.cn/open-apis";
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(30);
    private int maxRetries = 3;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getKolAppToken() {
        return kolAppToken;
    }

    public void setKolAppToken(String kolAppToken) {
        this.kolAppToken = kolAppToken;
    }

    public String getKolTableId() {
        return kolTableId;
    }

    public void setKolTableId(String kolTableId) {
        this.kolTableId = kolTableId;
    }

    public String getApiBase() {
        return apiBase;
    }

    public void setApiBase(String apiBase) {
        this.apiBase = apiBase;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}
