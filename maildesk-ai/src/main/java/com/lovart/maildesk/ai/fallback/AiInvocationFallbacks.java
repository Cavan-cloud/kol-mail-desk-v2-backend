package com.lovart.maildesk.ai.fallback;

import java.util.function.Supplier;

/**
 * Hooks invoked when no provider is configured or all providers fail.
 */
public record AiInvocationFallbacks<T>(Supplier<T> onNoProvider, Supplier<T> onAllProvidersFailed) {}
