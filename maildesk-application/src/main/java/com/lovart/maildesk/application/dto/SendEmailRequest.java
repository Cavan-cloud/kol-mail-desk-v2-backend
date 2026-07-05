package com.lovart.maildesk.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/gmail/send}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SendEmailRequest(
        @NotNull(message = "请指定达人")
        UUID kolId,
        @NotBlank(message = "请填写收件人")
        @Email(message = "收件人邮箱格式不正确")
        String to,
        List<@Email(message = "CC 邮箱格式不正确") String> ccEmails,
        @NotBlank(message = "请填写邮件主题")
        @Size(max = 500, message = "主题过长")
        String subject,
        @NotBlank(message = "请填写英文正文")
        @Size(max = 50_000, message = "英文正文过长")
        String englishBody,
        @Size(max = 200_000, message = "富文本正文过长")
        String englishBodyHtml,
        @Size(max = 50_000, message = "中文草稿过长")
        String chineseDraft,
        UUID templateId,
        @NotNull(message = "发送前必须确认已人工审核")
        Boolean reviewed
) {}
