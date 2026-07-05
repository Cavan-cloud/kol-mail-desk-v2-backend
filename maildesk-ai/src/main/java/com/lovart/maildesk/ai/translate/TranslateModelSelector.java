package com.lovart.maildesk.ai.translate;

/**
 * Picks 8k vs 32k (or DeepSeek flash vs pro) based on input length for translation.
 */
public final class TranslateModelSelector {

    /**
     * Rough character budget for {@code *-8k} models: input + output must fit the 8k context window
     * (see {@code 02-backend-design.md} §2.8).
     */
    public static final int LONG_TEXT_CHAR_THRESHOLD = 5_000;

    private TranslateModelSelector() {}

    public static String selectModel(String configuredModel, int textLength) {
        if (configuredModel == null || configuredModel.isBlank()) {
            return configuredModel;
        }
        if (textLength <= LONG_TEXT_CHAR_THRESHOLD) {
            return configuredModel;
        }
        return upgradeForLongText(configuredModel);
    }

    static String upgradeForLongText(String model) {
        if (model.contains("-8k")) {
            return model.replace("-8k", "-32k");
        }
        if (model.endsWith("-flash")) {
            return model.replace("-flash", "-pro");
        }
        return model;
    }
}
