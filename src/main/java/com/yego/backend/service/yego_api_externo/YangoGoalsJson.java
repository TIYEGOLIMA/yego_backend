package com.yego.backend.service.yego_api_externo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Limpia cada ítem de meta y alinea campos: si vienen bajo {@code common}, se copian a la raíz
 * para el cliente (steps, window, total_rides, multiplier_accounted_income).
 */
public final class YangoGoalsJson {

    private YangoGoalsJson() {}

    public static List<JsonNode> sanitizeGoalArray(JsonNode arrayNode) {
        List<JsonNode> out = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray()) {
            return out;
        }
        for (JsonNode el : arrayNode) {
            if (el != null && el.isObject()) {
                ObjectNode o = (ObjectNode) el.deepCopy();
                o.remove("id");
                o.remove("payment_info");
                o.remove("requirements");
                o.remove("is_availible");
                o.remove("is_available");
                hoistFromCommon(o);
                o.remove("orders");
                out.add(o);
            }
        }
        return out;
    }

    private static void hoistFromCommon(ObjectNode o) {
        JsonNode common = o.get("common");
        if (common == null || !common.isObject()) {
            return;
        }
        ObjectNode c = (ObjectNode) common;
        copyIfAbsent(o, c, "multiplier_accounted_income");
        copyIfAbsent(o, c, "total_rides");
        copyIfAbsent(o, c, "window");
        copyIfAbsent(o, c, "steps");
        copyIfAbsent(o, c, "is_multiplier_goal");
    }

    private static void copyIfAbsent(ObjectNode target, ObjectNode source, String field) {
        if (!target.has(field) && source.has(field)) {
            target.set(field, source.get(field));
        }
    }
}
