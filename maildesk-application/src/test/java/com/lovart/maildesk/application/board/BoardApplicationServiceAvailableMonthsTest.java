package com.lovart.maildesk.application.board;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.BoardSummaryDto;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardApplicationServiceAvailableMonthsTest {

    @Mock
    private KolMapper kols;

    @Mock
    private EmailMapper emails;

    @Mock
    private ProfileMapper profiles;

    private BoardApplicationService service;

    @BeforeEach
    void setUp() {
        service = new BoardApplicationService(kols, emails, profiles);
        when(profiles.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
    }

    @Test
    void getBoard_availableMonths_distinctDescendingYearMonth() {
        KolDO june = kol(LocalDate.of(2026, 6, 15));
        KolDO may = kol(LocalDate.of(2026, 5, 2));
        KolDO mayDup = kol(LocalDate.of(2026, 5, 20));
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(june, may, mayDup));
        when(emails.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        BoardSummaryDto result = service.getBoard("all", null, true, null, null, null, null);

        assertThat(result.availableMonths()).containsExactly("2026-06", "2026-05");
    }

    private static KolDO kol(LocalDate outreachAt) {
        KolDO kol = new KolDO();
        kol.setId(UUID.randomUUID());
        kol.setFeishuOutreachAt(outreachAt);
        return kol;
    }
}
