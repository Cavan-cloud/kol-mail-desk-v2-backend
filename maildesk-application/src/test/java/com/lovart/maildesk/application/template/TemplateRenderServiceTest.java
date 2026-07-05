package com.lovart.maildesk.application.template;

import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.enums.Platform;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRenderServiceTest {

    private TemplateRenderService service;

    @BeforeEach
    void setUp() {
        service = new TemplateRenderService();
    }

    @Test
    void render_replacesDocumentedVariables() {
        KolDO kol = new KolDO();
        kol.setName("Alex Creator");
        kol.setPrimaryPlatform(Platform.YOUTUBE);
        kol.setAgreedPrice(new BigDecimal("1500.00"));
        kol.setExternalProfileUrl("https://youtube.com/@alex");
        kol.setStage(KolStage.NEGOTIATING);

        String rendered = service.render(
                "Hi {{creator_name}} on {{platform}}, budget ${{quote}} — {{homepage_url}} by {{operator_name}}",
                new TemplateRenderContext(kol, "Chloe"));

        assertThat(rendered)
                .isEqualTo("Hi Alex Creator on youtube, budget $1500 — https://youtube.com/@alex by Chloe");
    }

    @Test
    void render_supportsSeedAliases() {
        KolDO kol = new KolDO();
        kol.setName("Mia");
        kol.setAgreedPrice(new BigDecimal("800"));

        String rendered = service.render(
                "Hi {{kol_name}}, budget ${{agreed_price}}",
                new TemplateRenderContext(kol, "王雨"));

        assertThat(rendered).isEqualTo("Hi Mia, budget $800");
    }

    @Test
    void render_leavesUnknownPlaceholdersUntouched() {
        String rendered = service.render(
                "Hello {{unknown_var}}",
                new TemplateRenderContext(new KolDO(), "Ops"));

        assertThat(rendered).isEqualTo("Hello {{unknown_var}}");
    }

    @Test
    void renderTemplate_replacesSubjectAndBody() {
        KolDO kol = new KolDO();
        kol.setName("Nova");

        TemplateRenderService.RenderedTemplate rendered = service.renderTemplate(
                "Subject for {{creator_name}}",
                "Body for {{kol_name}}",
                new TemplateRenderContext(kol, "Chloe"));

        assertThat(rendered.subject()).isEqualTo("Subject for Nova");
        assertThat(rendered.body()).isEqualTo("Body for Nova");
    }
}
