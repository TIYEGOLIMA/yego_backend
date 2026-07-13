package com.yego.backend.service.yego_marketing_mensajes.sender;

public record MarketingDeliveryResult(int enviados, int fallidos, int omitidos, int total) {

    public static MarketingDeliveryResult vacio() {
        return new MarketingDeliveryResult(0, 0, 0, 0);
    }
}
