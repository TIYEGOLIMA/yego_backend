package com.yego.backend.service.yego_gantt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Etiquetas reservadas que marcan tarea como privada (alineado con {@code taskPrivacy.ts} en frontend).
 */
public final class GanttTaskTagPrivacy {

    private GanttTaskTagPrivacy() {
    }

    public static boolean tagsIndicatePrivate(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return false;
        }
        for (String tag : tags) {
            String t = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
            if ("privada".equals(t) || "privado".equals(t) || "private".equals(t)
                    || t.startsWith("privada:") || t.startsWith("private:")) {
                return true;
            }
        }
        return false;
    }

    /** Lista nueva sin etiquetas reservadas de privacidad. */
    public static List<String> stripPrivacyTagLabels(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        return tags.stream()
                .filter(tag -> {
                    String t = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
                    return !"privada".equals(t) && !"privado".equals(t) && !"private".equals(t)
                            && !t.startsWith("privada:") && !t.startsWith("private:");
                })
                .toList();
    }
}
