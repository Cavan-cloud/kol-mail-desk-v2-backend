package com.lovart.maildesk.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST/PATCH /api/v1/templates}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TemplateUpsertRequest(
        @NotBlank(message = "请填写模板名称")
        @Size(max = 120, message = "模板名称过长")
        String name,
        @NotBlank(message = "请填写使用场景")
        @Size(max = 120, message = "场景描述过长")
        String scenario,
        @NotBlank(message = "请填写邮件主题")
        @Size(max = 500, message = "主题过长")
        String subject,
        @NotBlank(message = "请填写模板正文")
        @Size(max = 50_000, message = "正文过长")
        String body
) {}
