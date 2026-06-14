package com.bo.uagrm.negocio.strategy;

import java.math.BigDecimal;

public interface EstrategiaCalculoMulta {
    BigDecimal calcularMulta(int diasRetraso);
    int        getDiasGracia();
    BigDecimal getMontoPorDia();
    String     getNombreEstrategia();
}