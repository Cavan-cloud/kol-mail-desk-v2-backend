package com.lovart.maildesk.integration.feishu;

import com.lovart.maildesk.common.feishu.FeishuTabFilterMode;
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
    /** Bitable app token ({@code /base/{token}}) or legacy spreadsheet token. */
    private String kolAppToken = "";
    /** Optional single-table override ({@code tbl...}); empty = sync all month + regional tables. */
    private String kolTableId = "";
    /** {@code bitable} (default) or {@code sheet} for legacy spreadsheet workbooks. */
    private String syncSource = "bitable";
    /**
     * Tab/table selection: {@code roster} (all month tabs + regional), {@code recent-months}, or {@code all}.
     */
    private String tabFilter = "roster";
    /** Comma-separated regional roster tab names, e.g. {@code 欧美,拉美,韩国}. */
    private String regionalTabNames = "欧美,拉美,韩国";
    /** Rolling window size when {@code tab-filter=recent-months}. */
    private int sheetRecentMonths = 2;
    /** When true, do not cap merged KOL pairs per run ({@code maxRecords} ignored). */
    private boolean fullSync = true;
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

    public String getSyncSource() {
        return syncSource;
    }

    public void setSyncSource(String syncSource) {
        this.syncSource = syncSource;
    }

    public boolean isBitableSource() {
        return syncSource == null || syncSource.isBlank() || "bitable".equalsIgnoreCase(syncSource.trim());
    }

    public FeishuTabFilterMode tabFilterMode() {
        return FeishuTabFilterMode.fromConfig(tabFilter);
    }

    public String getTabFilter() {
        return tabFilter;
    }

    public void setTabFilter(String tabFilter) {
        this.tabFilter = tabFilter;
    }

    public String getRegionalTabNames() {
        return regionalTabNames;
    }

    public void setRegionalTabNames(String regionalTabNames) {
        this.regionalTabNames = regionalTabNames;
    }

    public java.util.List<String> resolvedRegionalTabNames() {
        if (regionalTabNames == null || regionalTabNames.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(regionalTabNames.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
    }

    public java.util.Set<String> resolvedRegionalTabNameSet() {
        return java.util.Set.copyOf(resolvedRegionalTabNames());
    }

    public int getSheetRecentMonths() {
        return sheetRecentMonths;
    }

    public void setSheetRecentMonths(int sheetRecentMonths) {
        this.sheetRecentMonths = sheetRecentMonths;
    }

    public boolean isFullSync() {
        return fullSync;
    }

    public void setFullSync(boolean fullSync) {
        this.fullSync = fullSync;
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
