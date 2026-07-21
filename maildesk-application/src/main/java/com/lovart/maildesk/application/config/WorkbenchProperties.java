package com.lovart.maildesk.application.config;

import com.lovart.maildesk.common.enums.CrossMailboxVisibility;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "maildesk.workbench")
public record WorkbenchProperties(
        /**
         * Who may read emails that other operators synced from their own Gmail.
         * Values: {@code own_only} | {@code leader_only} | {@code non_intern} (default).
         */
        @DefaultValue("non_intern") String crossMailboxVisibility) {

    public CrossMailboxVisibility visibilityMode() {
        return CrossMailboxVisibility.fromConfig(crossMailboxVisibility);
    }
}
