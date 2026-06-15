package com.bo.uagrm.negocio;

import com.bo.uagrm.datos.MultaDao;
import com.bo.uagrm.datos.PrestamoDao;
import com.bo.uagrm.datos.entity.Multa;
import com.bo.uagrm.datos.entity.Prestamo;
import com.bo.uagrm.datos.entity.PrestamoItem;
import com.bo.uagrm.negocio.strategy.EstrategiaCalculoMulta;
import com.bo.uagrm.negocio.strategy.MultaDocente;
import com.bo.uagrm.negocio.strategy.MultaEstudiante;
import com.bo.uagrm.negocio.strategy.MultaInvestigador;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Contexto del patrón Strategy para el cálculo de multas.
 * Responsabilidades:
 *  1. Marcar el préstamo como DEVUELTO en BD.
 *  2. Calcular días de retraso.
 *  3. Seleccionar la estrategia según el rol del usuario en ms_usuario.
 *  4. Calcular el monto de la multa con la estrategia elegida.
 *  5. Persistir la multa si corresponde (diasEfectivos > 0).
 *  6. Liberar ejemplares en ms_libros.
 */
public class DevolucionN {

    // ── Setter de estrategia
    @Setter
    private EstrategiaCalculoMulta estrategia;
    private final PrestamoDao prestamoDao = new PrestamoDao();
    private final MultaDao    multaDao    = new MultaDao();

    public EstrategiaCalculoMulta obtenerEstrategia(int usuarioId) {
        try {
            List<String> roles = MsClient.obtenerRoles(usuarioId);
            if (roles.contains("INVESTIGADOR")) return new MultaInvestigador();
            if (roles.contains("DOCENTE"))      return new MultaDocente();
        } catch (Exception e) {
            System.err.printf("[DevolucionN] No se pudo obtener rol del usuario %d: %s%n",
                    usuarioId, e.getMessage());
        }
        return new MultaEstudiante();
    }

    public Multa registrarDevolucion(int prestamoId) throws Exception {
        // 1. Verificar que el préstamo existe y está PENDIENTE
        Prestamo prestamo = prestamoDao.buscarPorId(prestamoId);
        if (prestamo == null)
            throw new IllegalArgumentException("Préstamo " + prestamoId + " no encontrado");
        if ("DEVUELTO".equals(prestamo.getEstado()))
            throw new IllegalStateException("El préstamo ya fue devuelto");

        // 2. Calcular días de retraso
        int diasRetraso = calcularDiasRetraso(prestamo);

        // 3. Seleccionar estrategia según rol del usuario
        if (estrategia == null) {
            estrategia = obtenerEstrategia(prestamo.getUsuarioId());
        }

        System.out.printf("[DevolucionN] Préstamo %d — estrategia: %s | retraso: %d días%n",
                prestamoId, estrategia.getNombreEstrategia(), diasRetraso);

        // 4. Calcular monto
        BigDecimal monto = estrategia.calcularMulta(diasRetraso);
        int diasGracia   = estrategia.getDiasGracia();
        int diasEfectivos = Math.max(0, diasRetraso - diasGracia);

        // 5. Marcar como devuelto en BD
        prestamoDao.devolverPrestamo(prestamoId);

        // 6. Persistir multa solo si hay retraso efectivo
        Multa multa = new Multa();
        multa.setPrestamoId(prestamoId);
        multa.setTipoCalculo(estrategia.getNombreEstrategia());
        multa.setDiasRetraso(diasRetraso);
        multa.setDiasGraciaAplicados(diasGracia);
        multa.setMontoPorDia(estrategia.getMontoPorDia());
        multa.setMontoCalculado(monto);

        if (diasEfectivos > 0) {
            int id = multaDao.registrar(multa);
            multa.setId(id);
            multa.setEstado("PENDIENTE");
            System.out.printf("[DevolucionN] Multa generada: %s Bs (ID %d)%n", monto, id);

            // Notificar multa al usuario por SSE
            try {
                MsClient.notificarMulta(
                        prestamo.getUsuarioId(),
                        prestamoId,
                        monto,
                        estrategia.getNombreEstrategia()
                );
            } catch (Exception e) {
                System.err.println("[DevolucionN] Warning: no se pudo notificar multa: " + e.getMessage());
            }
        } else {
            multa.setEstado("SIN_MULTA");
            System.out.printf("[DevolucionN] Devolución a tiempo — sin multa (días gracia: %d)%n", diasGracia);
        }

        // 7. Liberar ejemplares en ms_libros
        for (PrestamoItem item : prestamo.getItems()) {
            try {
                MsClient.actualizarNroEjemplar(item.getIdLibro(), +1);
            } catch (Exception e) {
                System.err.printf("[DevolucionN] Warning: no se pudo liberar libro %d: %s%n",
                        item.getIdLibro(), e.getMessage());
            }
        }

        return multa;
    }

    // ── Utilidad ──────────────────────────────────────────────────────────────

    private int calcularDiasRetraso(Prestamo prestamo) {
        if (prestamo.getFechaEntregaPrevista() == null) return 0;
        LocalDate entregaPrevista = ((java.sql.Date) prestamo.getFechaEntregaPrevista()).toLocalDate();
        LocalDate hoy = LocalDate.now();
        long dias = ChronoUnit.DAYS.between(entregaPrevista, hoy);
        return (int) Math.max(0, dias);
    }
}