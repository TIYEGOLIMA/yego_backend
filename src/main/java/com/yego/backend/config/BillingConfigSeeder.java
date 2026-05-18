package com.yego.backend.config;

import com.yego.backend.entity.yego_pro_ops.entities.BonusThreshold;
import com.yego.backend.entity.yego_pro_ops.entities.PaymentPercentage;
import com.yego.backend.repository.yego_pro_ops.BonusThresholdRepository;
import com.yego.backend.repository.yego_pro_ops.PaymentPercentageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingConfigSeeder implements CommandLineRunner {

    private final BonusThresholdRepository bonusThresholdRepository;
    private final PaymentPercentageRepository paymentPercentageRepository;

    private static final LocalDate DEFAULT_EFFECTIVE = LocalDate.of(2024, 5, 1);

    @Override
    public void run(String... args) {
        try {
            seedBonusThresholds();
            seedPaymentPercentages();
        } catch (Exception e) {
            log.warn("[BillingConfigSeeder] No se pudo inicializar la configuración. ¿Ejecutaste el SQL? Error: {}",
                e.getMessage());
        }
    }

    private void seedBonusThresholds() {
        if (bonusThresholdRepository.count() > 0) return;

        bonusThresholdRepository.save(BonusThreshold.builder()
            .minTrips(165).bonusAmount(new BigDecimal("300")).effectiveFrom(DEFAULT_EFFECTIVE).build());
        bonusThresholdRepository.save(BonusThreshold.builder()
            .minTrips(150).bonusAmount(new BigDecimal("115")).effectiveFrom(DEFAULT_EFFECTIVE).build());
        bonusThresholdRepository.save(BonusThreshold.builder()
            .minTrips(135).bonusAmount(new BigDecimal("100")).effectiveFrom(DEFAULT_EFFECTIVE).build());
        bonusThresholdRepository.save(BonusThreshold.builder()
            .minTrips(125).bonusAmount(new BigDecimal("50")).effectiveFrom(DEFAULT_EFFECTIVE).build());

        log.info("[BillingConfigSeeder] {} bonus thresholds seeded", 4);
    }

    private void seedPaymentPercentages() {
        if (paymentPercentageRepository.count() > 0) return;

        paymentPercentageRepository.save(PaymentPercentage.builder()
            .minValidatedTrips(140).percentage(0.6).effectiveFrom(DEFAULT_EFFECTIVE).build());
        paymentPercentageRepository.save(PaymentPercentage.builder()
            .minValidatedTrips(128).percentage(0.55).effectiveFrom(DEFAULT_EFFECTIVE).build());
        paymentPercentageRepository.save(PaymentPercentage.builder()
            .minValidatedTrips(117).percentage(0.5).effectiveFrom(DEFAULT_EFFECTIVE).build());
        paymentPercentageRepository.save(PaymentPercentage.builder()
            .minValidatedTrips(107).percentage(0.45).effectiveFrom(DEFAULT_EFFECTIVE).build());
        paymentPercentageRepository.save(PaymentPercentage.builder()
            .minValidatedTrips(100).percentage(0.4).effectiveFrom(DEFAULT_EFFECTIVE).build());
        paymentPercentageRepository.save(PaymentPercentage.builder()
            .minValidatedTrips(95).percentage(0.35).effectiveFrom(DEFAULT_EFFECTIVE).build());
        paymentPercentageRepository.save(PaymentPercentage.builder()
            .minValidatedTrips(90).percentage(0.3).effectiveFrom(DEFAULT_EFFECTIVE).build());

        log.info("[BillingConfigSeeder] {} payment percentages seeded", 7);
    }
}
