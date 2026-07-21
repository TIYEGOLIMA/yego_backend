package com.yego.backend.config.yego_pro_ops;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "yego.pro-ops")
public class YegoProOpsProperties {

    private String parkId;
    private final Yango yango = new Yango();

    @Getter
    @Setter
    public static class Yango {
        private String vehiclesUrl;
        private String vehicleDetailsUrl;
        private String qcHistoryUrl;
        private String driverPointsUrl;
        private String driversListUrl;
        private String contractorsUrl;
        private String driverIncomeUrl;
        private String driverDetailsUrl;
        private String driverCommonUrl;
        private String driverTransactionsListUrl;
        private String goalsUrlTemplate;
        private String ordersUrl;
        private String suggestionsUrl;
    }
}
