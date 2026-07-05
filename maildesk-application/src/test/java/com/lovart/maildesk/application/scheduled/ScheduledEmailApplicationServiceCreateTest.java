package com.lovart.maildesk.application.scheduled;

import com.lovart.maildesk.application.dto.ScheduledEmailCreateRequest;
import com.lovart.maildesk.common.enums.UserStatus;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import com.lovart.maildesk.domain.scheduled.entity.ScheduledEmailDO;
import com.lovart.maildesk.domain.scheduled.mapper.ScheduledEmailMapper;
import com.lovart.maildesk.domain.template.mapper.EmailTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledEmailApplicationServiceCreateTest {

    @Mock
    private ScheduledEmailMapper scheduledEmails;

    @Mock
    private KolMapper kols;

    @Mock
    private ProfileMapper profiles;

    @Mock
    private EmailTemplateMapper templates;

    private ScheduledEmailApplicationService service;
    private UUID userId;
    private UUID kolId;

    @BeforeEach
    void setUp() {
        service = new ScheduledEmailApplicationService(scheduledEmails, kols, profiles, templates);
        userId = UUID.randomUUID();
        kolId = UUID.randomUUID();
    }

    @Test
    void create_persistsScheduledRow() {
        ProfileDO profile = new ProfileDO();
        profile.setId(userId);
        profile.setStatus(UserStatus.ACTIVE.dbValue());
        when(profiles.selectById(userId)).thenReturn(profile);

        KolDO kol = new KolDO();
        kol.setId(kolId);
        kol.setName("Alice");
        kol.setEmail("alice@example.com");
        when(kols.selectById(kolId)).thenReturn(kol);
        when(scheduledEmails.insert(any(ScheduledEmailDO.class))).thenReturn(1);

        var result = service.create(
                userId,
                new ScheduledEmailCreateRequest(
                        kolId,
                        null,
                        "alice@example.com",
                        List.of(),
                        "Subject",
                        "Body",
                        "<p>Body</p>",
                        "中文",
                        OffsetDateTime.now().plusHours(1)));

        assertThat(result.kolId()).isEqualTo(kolId);
        verify(scheduledEmails).insert(any(ScheduledEmailDO.class));
    }

    @Test
    void create_rejectsPastScheduleTime() {
        ProfileDO profile = new ProfileDO();
        profile.setId(userId);
        profile.setStatus(UserStatus.ACTIVE.dbValue());
        when(profiles.selectById(userId)).thenReturn(profile);

        assertThatThrownBy(() -> service.create(
                        userId,
                        new ScheduledEmailCreateRequest(
                                kolId,
                                null,
                                "alice@example.com",
                                List.of(),
                                "Subject",
                                "Body",
                                null,
                                null,
                                OffsetDateTime.now().minusMinutes(1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("定时发送时间");
    }
}
