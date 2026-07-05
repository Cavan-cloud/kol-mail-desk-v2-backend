package com.lovart.maildesk.application.template;

import com.lovart.maildesk.common.enums.Platform;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces {@code {{variable}}} placeholders in template subject/body.
 */
@Service
public class TemplateRenderService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}");

    public String render(String template, TemplateRenderContext context) {
        if (template == null || template.isBlank()) {
            return template == null ? "" : template;
        }
        Map<String, String> variables = buildVariables(context);
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = normalizeKey(matcher.group(1));
            if (!variables.containsKey(key)) {
                continue;
            }
            String replacement = variables.get(key);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public RenderedTemplate renderTemplate(String subject, String body, TemplateRenderContext context) {
        return new RenderedTemplate(render(subject, context), render(body, context));
    }

    private static Map<String, String> buildVariables(TemplateRenderContext context) {
        KolDO kol = context.kol();
        Map<String, String> values = new LinkedHashMap<>();
        String kolName = text(kol == null ? null : kol.getName());
        String platform = firstNonBlank(
                platformSlug(kol == null ? null : kol.getPrimaryPlatform()),
                platformSlug(kol == null ? null : kol.getAgreedPlatform()));
        String homepage = kol == null ? null : kol.getExternalProfileUrl();
        String quote = formatPrice(kol == null ? null : kol.getAgreedPrice());
        String operatorName = text(context.operatorName());

        put(values, "creator_name", kolName);
        put(values, "kol_name", kolName);
        put(values, "platform", platform);
        put(values, "quote", quote);
        put(values, "agreed_price", quote);
        put(values, "homepage_url", homepage);
        put(values, "external_profile_url", homepage);
        put(values, "operator_name", operatorName);
        put(values, "kol_handle", kol == null ? "" : text(kol.getHandle()));
        if (kol != null && kol.getStage() != null) {
            put(values, "stage", kol.getStage().name().toLowerCase());
        }
        return values;
    }

    private static void put(Map<String, String> values, String key, String value) {
        values.put(normalizeKey(key), value == null ? "" : value);
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String platformSlug(Platform platform) {
        return platform == null ? null : platform.dbValue();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return "";
    }

    private static String formatPrice(BigDecimal price) {
        if (price == null) {
            return "";
        }
        return price.stripTrailingZeros().toPlainString();
    }

    public record RenderedTemplate(String subject, String body) {}
}
