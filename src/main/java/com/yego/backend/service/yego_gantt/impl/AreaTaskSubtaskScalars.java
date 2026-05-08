package com.yego.backend.service.yego_gantt.impl;

/**
 * Normalización de textos opcionales al persistir subtareas (objetivos, descripción corta).
 */
final class AreaTaskSubtaskScalars {

    private AreaTaskSubtaskScalars() {}

    static String objectivesOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > 4000 ? t.substring(0, 4000) : t;
    }

    static String normalizeDescriptionOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }
}
