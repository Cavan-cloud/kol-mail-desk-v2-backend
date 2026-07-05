package com.lovart.maildesk.application.scheduled;

import com.lovart.maildesk.common.enums.ScheduledEmailStatus;
import com.lovart.maildesk.common.enums.UserStatus;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import com.lovart.maildesk.domain.scheduled.entity.ScheduledEmailDO;
import com.lovart.maildesk.domain.scheduled.mapper.ScheduledEmailMapper;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.template.mapper.EmailTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledEmailApplicationServiceCancelTest {

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
    private UUID scheduledId;

    @BeforeEach
    void setUp() {
        service = new ScheduledEmailApplicationService(scheduledEmails, kols, profiles, templates);
        userId = UUID.randomUUID();
        scheduledId = UUID.randomUUID();
    }

    @Test
    void cancel_marksCancelledWhenScheduled() {
        ProfileDO profile = new ProfileDO();
        profile.setId(userId);
        profile.setStatus(UserStatus.ACTIVE.dbValue());
        when(profiles.selectById(userId)).thenReturn(profile);

        ScheduledEmailDO existing = new ScheduledEmailDO();
        existing.setId(scheduledId);
        existing.setUserId(userId);
        existing.setStatus(ScheduledEmailStatus.SCHEDULED.dbValue());
        when(scheduledEmails.selectById(scheduledId)).thenReturn(existing);

        service.cancel(userId, scheduledId);

        verify(scheduledEmails).updateById(any(ScheduledEmailDO.class));
    }

    @Test
    void cancel_rejectsProcessingRow() {
        ProfileDO profile = new ProfileDO();
        profile.setId(userId);
        profile.setStatus(UserStatus.ACTIVE.dbValue());
        when(profiles.selectById(userId)).thenReturn(profile);

        ScheduledEmailDO existing = new ScheduledEmailDO();
        existing.setId(scheduledId);
        existing.setUserId(userId);
        existing.setStatus(ScheduledEmailStatus.PROCESSING.dbValue());
        when(scheduledEmails.selectById(scheduledId)).thenReturn(existing);

        assertThatThrownBy(() -> service.cancel(userId, scheduledId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法取消");
    }
}
