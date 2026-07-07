package com.lovart.maildesk.integration.feishu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.domain.feishu.FeishuBitableRecord;
import com.lovart.maildesk.domain.feishu.FeishuConfigCheckResult;
import com.lovart.maildesk.domain.feishu.FeishuSheetMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FeishuClientImplTest {

    private static final String API_BASE = "https://feishu.test/open-apis";

    private FeishuProperties properties;
    private MockRestServiceServer server;
    private FeishuClientImpl client;

    @BeforeEach
    void setUp() {
        properties = new FeishuProperties();
        properties.setAppId("cli_test");
        properties.setAppSecret("secret_test");
        properties.setKolAppToken("sheet_token_abc");
        properties.setKolTableId("tbl_test");
        properties.setSyncSource("sheet");
        properties.setApiBase(API_BASE);

        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new FeishuClientImpl(properties, new ObjectMapper(), restTemplate);
    }

    @Test
    void isConfigured_requiresAppCredentialsAndKolToken() {
        assertThat(client.isConfigured()).isTrue();

        properties.setKolAppToken("");
        client = new FeishuClientImpl(properties, new ObjectMapper());
        assertThat(client.isConfigured()).isFalse();
    }

    @Test
    void verifyConfiguration_returnsNotConfiguredWhenMissingEnv() {
        properties.setAppSecret("");
        client = new FeishuClientImpl(properties, new ObjectMapper());

        FeishuConfigCheckResult result = client.verifyConfiguration();

        assertThat(result.ok()).isFalse();
        assertThat(result.status()).isEqualTo("not_configured");
        assertThat(result.missingConfigKeys()).contains("FEISHU_APP_SECRET");
    }

    @Test
    void listSheets_fetchesTenantTokenThenQueriesTabs() {
        expectToken();
        server.expect(requestTo(API_BASE + "/sheets/v3/spreadsheets/sheet_token_abc/sheets/query"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer tok_cached"))
                .andRespond(withSuccess("""
                        {"code":0,"data":{"sheets":[
                          {"sheet_id":"sh1","title":"3月","grid_properties":{"row_count":120,"column_count":18}},
                          {"sheet_id":"sh2","title":"欧美","grid_properties":{"row_count":80,"column_count":12}}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        List<FeishuSheetMeta> sheets = client.listSheets();

        assertThat(sheets).hasSize(2);
        assertThat(sheets.get(0).sheetId()).isEqualTo("sh1");
        assertThat(sheets.get(1).title()).isEqualTo("欧美");
        server.verify();
    }

    @Test
    void readSheetValues_requestsToStringRenderedRange() {
        expectToken();
        server.expect(requestTo(API_BASE + "/sheets/v2/spreadsheets/sheet_token_abc/values/sh1!A1:B2?valueRenderOption=ToString"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"data":{"valueRange":{"values":[
                          ["邮箱","运营"],
                          ["a@example.com","王雨"]
                        ]}}}
                        """, MediaType.APPLICATION_JSON));

        FeishuSheetMeta sheet = new FeishuSheetMeta("sh1", "3月", 2, 2);
        List<List<Object>> rows = client.readSheetValues("sheet_token_abc", sheet, null);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).get(0)).isEqualTo("a@example.com");
        server.verify();
    }

    @Test
    void listBitableRecords_paginatesUntilHasMoreFalse() {
        expectToken();
        server.expect(requestTo(API_BASE + "/bitable/v1/apps/appTok/tables/tblX/records?page_size=100"))
                .andRespond(withSuccess("""
                        {"code":0,"data":{"has_more":true,"page_token":"p2","items":[
                          {"record_id":"rec1","fields":{"达人邮箱":"a@example.com"}}
                        ]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(API_BASE + "/bitable/v1/apps/appTok/tables/tblX/records?page_size=100&page_token=p2"))
                .andRespond(withSuccess("""
                        {"code":0,"data":{"has_more":false,"items":[
                          {"record_id":"rec2","fields":{"达人邮箱":"b@example.com"}}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        List<FeishuBitableRecord> records = client.listBitableRecords("appTok", "tblX", null);

        assertThat(records).hasSize(2);
        assertThat(records.get(1).fields().get("达人邮箱")).isEqualTo("b@example.com");
        server.verify();
    }

    @Test
    void verifyConfiguration_listsSheetsAndSummarizes() {
        expectToken();
        server.expect(requestTo(API_BASE + "/sheets/v3/spreadsheets/sheet_token_abc/sheets/query"))
                .andRespond(withSuccess("""
                        {"code":0,"data":{"sheets":[
                          {"sheet_id":"sh1","title":"3月","grid_properties":{"row_count":10,"column_count":5}}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        FeishuConfigCheckResult result = client.verifyConfiguration();

        assertThat(result.ok()).isTrue();
        assertThat(result.status()).isEqualTo("checked");
        assertThat(result.sheetCount()).isEqualTo(1);
        assertThat(result.sheetTitles()).containsExactly("3月");
        server.verify();
    }

    @Test
    void listBitableTables_paginatesUntilHasMoreFalse() {
        properties.setSyncSource("bitable");
        properties.setKolAppToken("appTok");
        expectToken();
        server.expect(requestTo(API_BASE + "/bitable/v1/apps/appTok/tables?page_size=100"))
                .andRespond(withSuccess("""
                        {"code":0,"data":{"has_more":false,"items":[
                          {"table_id":"tbl7","name":"7月"},
                          {"table_id":"tblEu","name":"欧美"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        List<com.lovart.maildesk.domain.feishu.FeishuBitableTableMeta> tables =
                client.listBitableTables("appTok");

        assertThat(tables).hasSize(2);
        assertThat(tables.get(0).name()).isEqualTo("7月");
        server.verify();
    }

    @Test
    void verifyConfiguration_listsBitableTablesWhenBitableSource() {
        properties.setSyncSource("bitable");
        properties.setKolAppToken("app_token_xyz");
        expectToken();
        server.expect(requestTo(API_BASE + "/bitable/v1/apps/app_token_xyz/tables?page_size=100"))
                .andRespond(withSuccess("""
                        {"code":0,"data":{"has_more":false,"items":[
                          {"table_id":"tbl7","name":"7月"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        FeishuConfigCheckResult result = client.verifyConfiguration();

        assertThat(result.ok()).isTrue();
        assertThat(result.sheetTitles()).containsExactly("7月");
        server.verify();
    }

    @Test
    void columnLetter_matchesExcelNotation() {
        assertThat(FeishuClientImpl.columnLetter(1)).isEqualTo("A");
        assertThat(FeishuClientImpl.columnLetter(26)).isEqualTo("Z");
        assertThat(FeishuClientImpl.columnLetter(27)).isEqualTo("AA");
    }

    private void expectToken() {
        server.expect(requestTo(API_BASE + "/auth/v3/tenant_access_token/internal"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"code":0,"tenant_access_token":"tok_cached","expire":7200}
                        """, MediaType.APPLICATION_JSON));
    }
}
