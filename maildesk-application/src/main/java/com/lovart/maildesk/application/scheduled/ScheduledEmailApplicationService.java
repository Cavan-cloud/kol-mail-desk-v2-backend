package com.lovart.maildesk.application.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.PageMetaDto;
import com.lovart.maildesk.application.dto.ScheduledEmailDto;
import com.lovart.maildesk.application.dto.ScheduledEmailListResponseDto;
import com.lovart.maildesk.application.support.EntityMappers;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.scheduled.entity.ScheduledEmailDO;
import com.lovart.maildesk.domain.scheduled.mapper.ScheduledEmailMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ScheduledEmailApplicationService {

    private final ScheduledEmailMapper scheduledEmails;
    private final KolMapper kols;

    public ScheduledEmailApplicationService(ScheduledEmailMapper scheduledEmails, KolMapper kols) {
        this.scheduledEmails = scheduledEmails;
        this.kols = kols;
    }

    @Transactional(readOnly = true)
    public ScheduledEmailListResponseDto listForUser(UUID userId) {
        List<ScheduledEmailDO> rows = scheduledEmails.selectList(
                new LambdaQueryWrapper<ScheduledEmailDO>()
                        .eq(ScheduledEmailDO::getUserId, userId)
                        .orderByDesc(ScheduledEmailDO::getScheduledAt));

        Map<UUID, String> kolNames = loadKolNames(rows);
        List<ScheduledEmailDto> data = rows.stream()
                .map(row -> EntityMappers.toScheduledEmailDto(
                        row,
                        row.getKolId() == null ? null : kolNames.get(row.getKolId())
                ))
                .toList();
        PageMetaDto page = new PageMetaDto(1, data.size(), data.size());
        return new ScheduledEmailListResponseDto(data, page);
    }

    private Map<UUID, String> loadKolNames(List<ScheduledEmailDO> rows) {
        Map<UUID, String> names = new HashMap<>();
        for (ScheduledEmailDO row : rows) {
            UUID kolId = row.getKolId();
            if (kolId == null || names.containsKey(kolId)) {
                continue;
            }
            KolDO kol = kols.selectById(kolId);
            if (kol != null) {
                names.put(kolId, kol.getName() != null ? kol.getName() : kol.getEmail());
            }
        }
        return names;
    }
}
