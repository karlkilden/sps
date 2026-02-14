package com.kildeen.sps;

/**
 * Thrown when required configuration is missing or invalid.
 * Provides clear, actionable error messages for operators.
 */
public class ConfigurationException extends RuntimeException {

    private final String configKey;
    private final String suggestion;

    public ConfigurationException(String configKey, String message) {
        this(configKey, message, null);
    }

    public ConfigurationException(String configKey, String message, String suggestion) {
        super(formatMessage(configKey, message, suggestion));
        this.configKey = configKey;
        this.suggestion = suggestion;
    }

    private static String formatMessage(String configKey, String message, String suggestion) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║  CONFIGURATION ERROR                                         ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║  Missing: %-51s ║%n", configKey));
        sb.append(String.format("║  Details: %-51s ║%n", truncate(message, 51)));
        if (suggestion != null) {
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║  Fix: %-55s ║%n", truncate(suggestion, 55)));
        }
        sb.append("╚══════════════════════════════════════════════════════════════╝");
        return sb.toString();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getSuggestion() {
        return suggestion;
    }

    /**
     * Validates that a required configuration value is present.
     *
     * @param value the value to check
     * @param configKey the name of the configuration key
     * @param suggestion how to fix it
     * @param <T> the type of value
     * @return the value if present
     * @throws ConfigurationException if value is null
     */
    public static <T> T requireNonNull(T value, String configKey, String suggestion) {
        if (value == null) {
            throw new ConfigurationException(configKey, "Required configuration is missing", suggestion);
        }
        return value;
    }

    /**
     * Validates that a required string is present and non-empty.
     */
    public static String requireNonEmpty(String value, String configKey, String suggestion) {
        if (value == null || value.isBlank()) {
            throw new ConfigurationException(configKey, "Required configuration is missing or empty", suggestion);
        }
        return value;
    }
}
