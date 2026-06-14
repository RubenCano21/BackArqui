package com.bo.uagrm.negocio;

import com.bo.uagrm.datos.PrestamoDao;
import com.bo.uagrm.datos.entity.Prestamo;
import com.bo.uagrm.datos.entity.PrestamoItem;

import java.util.List;

public class PrestamoN {


    private final PrestamoDao dao = new PrestamoDao();

    public List<Prestamo> listarPrestamos() throws Exception {
        return dao.listarPrestamos();
    }

    public List<Prestamo> listarPorUsuario(int usuarioId) throws Exception {
        return dao.listarPorUsuario(usuarioId);
    }

    public Prestamo buscarPorId(int id) throws Exception {
        return dao.buscarPorId(id);
    }

    public Prestamo registrarPrestamo(Prestamo nuevo) throws Exception {
        // Validaciones básicas
        if (nuevo.getUsuarioId() <= 0)
            throw new IllegalArgumentException("usuarioId es obligatorio");
        if (nuevo.getItems() == null || nuevo.getItems().isEmpty())
            throw new IllegalArgumentException("El préstamo debe llevar al menos un libro");

        // Validar que el estudiante existe en ms_usuario
        MsClient.validarUsuarioExiste(nuevo.getUsuarioId());

        // Validar que el estudiante no tenga un préstamo PENDIENTE activo
        if (dao.tienePrestamoPendiente(nuevo.getUsuarioId())) {
            throw new IllegalStateException(
                    "El estudiante ya tiene un préstamo pendiente. " +
                            "Debe devolver los libros antes de realizar un nuevo préstamo."
            );
        }

        // Validar que cada libro existe y tiene ejemplares disponibles
        for (PrestamoItem item : nuevo.getItems()) {
            MsClient.validarLibroExiste(item.getIdLibro());
            validarEjemplarDisponible(item.getIdLibro());
        }

        try {
            Prestamo result = dao.registrarPrestamo(nuevo);
            // Decrementar ejemplares disponibles en ms_libros
            for (PrestamoItem item : result.getItems()) {
                try {
                    MsClient.actualizarNroEjemplar(item.getIdLibro(), -1);
                } catch (Exception e) {
                    System.err.println("[PrestamoN] Warning: no se pudo decrementar ejemplar del libro "
                            + item.getIdLibro() + ": " + e.getMessage());
                }
            }
            return result;
        } catch (java.sql.BatchUpdateException e) {
            if (e.getMessage() != null && e.getMessage().contains("uq_prestamo_libro")) {
                throw new IllegalStateException("No se permiten libros repetidos en el mismo préstamo");
            }
            throw e;
        }
    }

    public boolean devolverPrestamo(int id) throws Exception {
        // Obtener items ANTES de marcar como devuelto para poder incrementar ejemplares
        Prestamo prestamo = dao.buscarPorId(id);
        boolean result = dao.devolverPrestamo(id);
        if (result && prestamo != null) {
            for (PrestamoItem item : prestamo.getItems()) {
                try {
                    MsClient.actualizarNroEjemplar(item.getIdLibro(), +1);
                } catch (Exception e) {
                    System.err.println("[PrestamoN] Warning: no se pudo incrementar ejemplar del libro "
                            + item.getIdLibro() + ": " + e.getMessage());
                }
            }
        }
        return result;
    }

    public boolean agregarItem(int prestamoId, int libroId) throws Exception {
        if (libroId <= 0) throw new IllegalArgumentException("libroId inválido");

        // Validar que el libro existe y tiene ejemplares disponibles
        MsClient.validarLibroExiste(libroId);
        validarEjemplarDisponible(libroId);

        try {
            return dao.agregarItem(prestamoId, libroId);
        } catch (java.sql.SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("uq_prestamo_libro")) {
                throw new IllegalStateException("Ese libro ya está registrado en este préstamo");
            }
            throw e;
        }
    }

    public boolean eliminarItem(int prestamoId, int libroId) throws Exception {
        return dao.eliminarItem(prestamoId, libroId);
    }

    public boolean cancelarPrestamo(int id) throws Exception {
        return dao.cancelarPrestamo(id);
    }

    // ── Validación interna ────────────────────────────────────────────────────

    /**
     * Verifica que haya al menos un ejemplar físico disponible para prestar.
     * nroEjemplar (total copias) - prestamos activos >= 1
     */
    private void validarEjemplarDisponible(int libroId) throws Exception {
        int disponibles = MsClient.getNroEjemplar(libroId);
        if (disponibles <= 0) {
            String titulo = MsClient.getTituloLibro(libroId);
            throw new IllegalStateException(
                    "El libro \"" + titulo + "\" no tiene ejemplares disponibles en la biblioteca"
            );
        }
    }
}