package com.yego.backend.service.yego_gantt;

import java.util.List;

/** Etiquetas reservadas de privacidad (alineado con {@code taskPrivacy.ts} en frontend). */
public interface GanttTaskTagPrivacyService {

    boolean tagsIndicatePrivate(List<String> tags);

    List<String> stripPrivacyTagLabels(List<String> tags);
}
