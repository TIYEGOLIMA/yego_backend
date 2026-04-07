package com.yego.backend.service.yego_api_externo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Quita {@code id} y {@code payment_info} del objeto de cada goal (nivel raíz del ítem).
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
                o.remove("multiplier_accounted_income");
                o.remove("is_multiplier_goal");
                out.add(o);
            }
        }
        return out;
    }
}
