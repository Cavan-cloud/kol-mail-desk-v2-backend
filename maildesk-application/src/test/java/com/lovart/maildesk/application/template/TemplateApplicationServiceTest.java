package com.lovart.maildesk.application.template;

import com.lovart.maildesk.application.dto.TemplateUpsertRequest;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.template.entity.EmailTemplateDO;
import com.lovart.maildesk.domain.template.mapper.EmailTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateApplicationServiceTest {

    @Mock
    private EmailTemplateMapper templates;

    private TemplateApplicationService service;
    private UUID userId;
    private UUID otherUserId;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        service = new TemplateApplicationService(templates);
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        templateId = UUID.randomUUID();
    }

    @Test
    void listTemplates_returnsOnlyCurrentUserRows() {
        EmailTemplateDO mine = sampleTemplate(userId);
        when(templates.selectList(any())).thenReturn(List.of(mine));

        var response = service.listTemplates(userId);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().createdBy()).isEqualTo(userId);
    }

    @Test
    void createTemplate_insertsRow() {
        ArgumentCaptor<EmailTemplateDO> captor = ArgumentCaptor.forClass(EmailTemplateDO.class);
        when(templates.selectById(any())).thenAnswer(invocation -> {
            EmailTemplateDO row = sampleTemplate(userId);
            row.setId(templateId);
            row.setName("初次触达");
            row.setScenario("冷启动外联");
            return row;
        });

        var created = service.createTemplate(userId, upsertRequest());

        assertThat(created.name()).isEqualTo("初次触达");
        verify(templates).insert(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("初次触达");
        assertThat(captor.getValue().getUsedCount()).isZero();
    }

    @Test
    void updateTemplate_rejectsForeignTemplate() {
        EmailTemplateDO foreign = sampleTemplate(otherUserId);
        foreign.setId(templateId);
        when(templates.selectById(templateId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.updateTemplate(userId, templateId, upsertRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void deleteTemplate_softDeletesOwnedTemplate() {
        EmailTemplateDO mine = sampleTemplate(userId);
        mine.setId(templateId);
        when(templates.selectById(templateId)).thenReturn(mine);

        service.deleteTemplate(userId, templateId);

        verify(templates).deleteById(templateId);
    }

    @Test
    void deleteTemplate_rejectsMissingTemplate() {
        when(templates.selectById(templateId)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteTemplate(userId, templateId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("NOT_FOUND");

        verify(templates, never()).deleteById(templateId);
    }

    private static EmailTemplateDO sampleTemplate(UUID ownerId) {
        EmailTemplateDO row = new EmailTemplateDO();
        row.setId(UUID.randomUUID());
        row.setName("Seed");
        row.setScenario("outreach");
        row.setSubject("Subject");
        row.setBody("Body");
        row.setCreatedBy(ownerId);
        row.setUsedCount(0);
        return row;
    }

    private static TemplateUpsertRequest upsertRequest() {
        return new TemplateUpsertRequest(
                "初次触达",
                "冷启动外联",
                "Collaboration with {{creator_name}}",
                "Hi {{creator_name}}, let's collaborate.");
    }
}
