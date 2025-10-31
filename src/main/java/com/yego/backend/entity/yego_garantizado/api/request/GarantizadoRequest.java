package com.yego.backend.entity.yego_garantizado.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GarantizadoRequest {
    private String licencia;
    private boolean exito;
    private int viajes;
    private int tarifas;        // En centavos
    private int pagoSinEfectivo; // En centavos
    private int comYango;       // En centavos
    private int comYego;        // En centavos
    private int boSemAnt;       // En centavos
    private int boSemAct;       // En centavos
    private String horasTrabajadas;
    private Integer horasTrabajadasEntero;
    private boolean brandeo;    // true = con brandeo, false = sin brandeo
}
