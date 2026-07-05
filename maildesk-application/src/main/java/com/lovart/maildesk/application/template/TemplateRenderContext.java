package com.lovart.maildesk.application.template;

import com.lovart.maildesk.domain.kol.entity.KolDO;

/**
 * Variable source for {@link TemplateRenderService}.
 */
public record TemplateRenderContext(
        KolDO kol,
        String operatorName
) {}
