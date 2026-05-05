package com.yego.backend.service.yego_gantt.impl;

import com.yego.backend.service.yego_gantt.GanttTaskTagPrivacyService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class GanttTaskTagPrivacyServiceImpl implements GanttTaskTagPrivacyService {

    @Override
    public boolean tagsIndicatePrivate(List<String> tags) {
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

    @Override
    public List<String> stripPrivacyTagLabels(List<String> tags) {
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
