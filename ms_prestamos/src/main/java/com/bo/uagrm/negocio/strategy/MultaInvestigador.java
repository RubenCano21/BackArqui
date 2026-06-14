package com.bo.uagrm.negocio.strategy;

import java.math.BigDecimal;

/**
 * Estrategia concreta: INVESTIGADOR
 * Sin días de gracia, multa de 10.00 Bs/día (libros especializados).
 */
public class MultaInvestigador implements EstrategiaCalculoMulta {

    private static final int        DIAS_GRACIA   = 0;
    private static final BigDecimal MONTO_POR_DIA = new BigDecimal("10.00");

    @Override
    public BigDecimal calcularMulta(int diasRetraso) {
        int diasEfectivos = Math.max(0, diasRetraso - DIAS_GRACIA);
        return MONTO_POR_DIA.multiply(BigDecimal.valueOf(diasEfectivos));
    }

    @Override public int        getDiasGracia()       { return DIAS_GRACIA; }
    @Override public BigDecimal getMontoPorDia()       { return MONTO_POR_DIA; }
    @Override public String     getNombreEstrategia()  { return "INVESTIGADOR"; }
}