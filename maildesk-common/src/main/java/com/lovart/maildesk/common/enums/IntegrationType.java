package com.lovart.maildesk.common.enums;

/**
 * Discriminator for {@code integration_credentials.type} (CHECK in V11):
 * {@code 'google' | 'feishu' | 'kimi'}. P1-T04 only wires {@link #GOOGLE}.
 */
public enum IntegrationType {
    GOOGLE,
    FEISHU,
    KIMI;

    public String dbValue() {
        return name().toLowerCase();
    }
}
