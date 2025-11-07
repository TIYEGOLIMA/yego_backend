package com.yego.backend.service.yego_premiun.impl;

import com.yego.backend.service.yego_premiun.FlotaLookupService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FlotaLookupServiceImpl implements FlotaLookupService {

    private static final Map<String, String> FLOTAS = Map.ofEntries(
            Map.entry("96f5a1e493b6484e88d7fc2e3bb8cbdb", "Yego Bucaramanga"),
            Map.entry("7ca266b7f3774ffc9a89b5b261adc62c", "Yego Cúcuta"),
            Map.entry("08e20910d81d42658d4334d3f6d10ac0", "Yego Lima"),
            Map.entry("56e4607dfc354e0a9cde4f0aa7973003", "Yego Arequipa"),
            Map.entry("ef21f793358144f589aabcbeb8bd7d50", "Yego Barranquilla"),
            Map.entry("c054c8b5dfe14e75b882943b2a252706", "Yego Black"),
            Map.entry("f4ac6fdbf26043dfabdd3315bb4d679e", "Yego Bogotá"),
            Map.entry("05b1c831e66f41a9a87f5f3fa0a186ae", "Yego Cali"),
            Map.entry("bed8509b67514379866e2907d72902a3", "Yego Cargo Lima"),
            Map.entry("8d3b13bd6e584a3c98c3a9d9825cf7d2", "Yego Delivery Barranquilla"),
            Map.entry("962afaa34db6420fb03b7ae464f6a061", "Yego Delivery Lima"),
            Map.entry("6a087c4492ae49759bb23592212aa189", "Yego Delivery Medellín"),
            Map.entry("c58110bc70244430a70a8126fc69f22c", "Yego Líderes"),
            Map.entry("e081e2df33a74073992c859638bdf683", "Yego Medellín"),
            Map.entry("5921e55cc5d042d28747dd722608955a", "Yego Prime"),
            Map.entry("64085dd85e124e2c808806f70d527ea8", "Yego Pro"),
            Map.entry("ff424287c4bd4cbba6066962951a121f", "Yego Promi"),
            Map.entry("851e30755bba4d298e2e837f571b4ab8", "Yego Trujillo"),
            Map.entry("e3e07c00ed914f82a59c03283a178d6e", "Yego TukTuk"),
            Map.entry("ae57aaedeacd41eb9fdbe1ff7a89a3f2", "Yego ,"),
            Map.entry("2e39f6699c854bc49cc75197431fe25c", "Yego ."),
            Map.entry("fafd623109d740f8a1f15af7c3dd86c6", "Yegó mi auto")
    );

    @Override
    public String obtenerNombreFlota(String parkId) {
        if (parkId == null || parkId.isBlank()) {
            return "Flota desconocida";
        }
        return FLOTAS.getOrDefault(parkId, "Flota " + parkId);
    }
}

