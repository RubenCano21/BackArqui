package com.bo.uagrm.negocio.strategy;

import com.bo.uagrm.negocio.MsClient;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

public class Context {

    @Setter
    private EstrategiaCalculoMulta estrategia;

    public Context() {}

    public BigDecimal ejecutarCalculo(int diasRetraso) {
        return estrategia.calcularMulta(diasRetraso);
    }

    public EstrategiaCalculoMulta getEstrategia() {
            return estrategia;
    }
    public int  getDiasGracia(){
        return estrategia.getDiasGracia();
    }
    public BigDecimal getMontoPorDia(){
        return estrategia.getMontoPorDia();
    }

    public String     getNombreEstrategia(){
        return estrategia.getNombreEstrategia();
    }


    public EstrategiaCalculoMulta obtenerEstrategia(int usuarioId) {
        try {
            List<String> roles = MsClient.obtenerRoles(usuarioId);
            if (roles.contains("INVESTIGADOR")) return new MultaInvestigador();
            else if (roles.contains("DOCENTE"))      return new MultaDocente();
            else this.estrategia = new MultaEstudiante();
        } catch (Exception e) {
            System.err.printf("[Context] No se pudo obtener rol del usuario %d: %s%n",
                    usuarioId, e.getMessage());
            this.estrategia = new MultaEstudiante();
        }
        return this.estrategia;
    }

}
