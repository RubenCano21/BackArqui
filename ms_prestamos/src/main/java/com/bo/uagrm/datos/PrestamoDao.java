package com.bo.uagrm.datos;

import com.bo.uagrm.datos.entity.Prestamo;
import com.bo.uagrm.datos.entity.PrestamoItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PrestamoDao {
    private static final boolean DEV_MODE = true;
    private static final int DEV_DIAS_ENTREGA = 3;

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    private Prestamo mapPrestamo(ResultSet rs) throws SQLException {
        Prestamo p = new Prestamo();
        p.setId(rs.getInt("id"));
        p.setFechaPrestamo(rs.getDate("fecha_prestamo"));
        p.setFechaEntregaPrevista(rs.getDate("fecha_entrega_prevista"));
        p.setFechaDevolucionReal(rs.getDate("fecha_devolucion_real"));
        p.setEstado(rs.getString("estado"));
        p.setUsuarioId(rs.getInt("usuario_id"));
        return p;
    }

    private PrestamoItem mapItem(ResultSet rs) throws SQLException {
        PrestamoItem item = new PrestamoItem();
        item.setId(rs.getInt("id"));
        item.setIdPrestamo(rs.getInt("prestamo_id"));
        item.setIdLibro(rs.getInt("libro_id"));
        return item;
    }

    // ── Listar items de un préstamo ───────────────────────────────────────────

    public List<PrestamoItem> listarItemsPorPrestamo(int prestamoId) throws Exception {
        List<PrestamoItem> items = new ArrayList<>();
        String sql = "SELECT * FROM prestamo_items WHERE prestamo_id = ? ORDER BY id";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, prestamoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapItem(rs));
            }
        }
        return items;
    }

    public List<Prestamo> listarPrestamos() throws Exception {
        List<Prestamo> lista = new ArrayList<>();
        String sql = "SELECT * FROM prestamos ORDER BY id";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Prestamo p = mapPrestamo(rs);
                p.setItems(listarItemsPorPrestamo(p.getId()));
                lista.add(p);
            }
        }
        return lista;
    }

    public List<Prestamo> listarPorUsuario(int usuarioId) throws Exception {
        List<Prestamo> lista = new ArrayList<>();
        String sql = "SELECT * FROM prestamos WHERE usuario_id = ? ORDER BY fecha_prestamo DESC";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Prestamo p = mapPrestamo(rs);
                    p.setItems(listarItemsPorPrestamo(p.getId()));
                    lista.add(p);
                }
            }
        }
        return lista;
    }


    public Prestamo buscarPorId(int id) throws Exception {
        String sql = "SELECT * FROM prestamos WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Prestamo p = mapPrestamo(rs);
                    p.setItems(listarItemsPorPrestamo(p.getId()));
                    return p;
                }
            }
        }
        return null;
    }


    public int contarPrestamosActivosPorLibro(int libroId) throws Exception {
        String sql = "SELECT COUNT(*) FROM prestamo_items pi " +
                "INNER JOIN prestamos p ON p.id = pi.prestamo_id " +
                "WHERE pi.libro_id = ? AND p.estado = 'PENDIENTE'";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, libroId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }


    public boolean tienePrestamoPendiente(int usuarioId) throws Exception {
        String sql = "SELECT 1 FROM prestamos WHERE usuario_id = ? AND estado = 'PENDIENTE' LIMIT 1";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ── Registrar préstamo + items en una sola transacción ───────────────────

    public Prestamo registrarPrestamo(Prestamo nuevo) throws Exception {
        if (nuevo.getItems() == null || nuevo.getItems().isEmpty()) {
            throw new IllegalArgumentException("El préstamo debe tener al menos un libro");
        }

        // DEV_MODE: fecha_entrega_prevista = hoy + DEV_DIAS_ENTREGA para facilitar pruebas de multas
        String sqlPrestamo = DEV_MODE
                ? "INSERT INTO prestamos(fecha_prestamo, fecha_entrega_prevista, estado, usuario_id) " +
                  "VALUES (CURRENT_DATE, CURRENT_DATE + " + DEV_DIAS_ENTREGA + ", 'PENDIENTE', ?) " +
                  "RETURNING id, fecha_prestamo, fecha_entrega_prevista, estado"
                : "INSERT INTO prestamos(fecha_prestamo, estado, usuario_id) " +
                  "VALUES (CURRENT_DATE, 'PENDIENTE', ?) RETURNING id, fecha_prestamo, estado";
        String sqlItem     = "INSERT INTO prestamo_items(prestamo_id, libro_id) VALUES (?, ?)";

        Connection conn = ConnectionDB.getConnection();
        conn.setAutoCommit(false);  // inicio de transacción
        try {
            // 1. Insertar cabecera
            int prestamoId;
            try (PreparedStatement ps = conn.prepareStatement(sqlPrestamo)) {
                ps.setInt(1, nuevo.getUsuarioId());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    prestamoId       = rs.getInt("id");
                    nuevo.setId(prestamoId);
                    nuevo.setFechaPrestamo(rs.getDate("fecha_prestamo"));
                    nuevo.setEstado(rs.getString("estado"));
                    if (DEV_MODE) nuevo.setFechaEntregaPrevista(rs.getDate("fecha_entrega_prevista"));
                }
            }

            // 2. Insertar items — la constraint UNIQUE en BD rechaza libros duplicados
            try (PreparedStatement ps = conn.prepareStatement(sqlItem)) {
                for (PrestamoItem item : nuevo.getItems()) {
                    ps.setInt(1, prestamoId);
                    ps.setInt(2, item.getIdLibro());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();

            // 3. Recargar items con sus IDs generados
            nuevo.setItems(listarItemsPorPrestamo(prestamoId));
            return nuevo;

        } catch (Exception e) {
            conn.rollback();  // si algo falla → se revierte
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    // ── Devolver préstamo (cambiar estado a DEVUELTO + fecha_devolucion_real) ──

    public boolean devolverPrestamo(int id) throws Exception {
        Prestamo p = buscarPorId(id);
        if (p == null) throw new IllegalArgumentException("Préstamo no encontrado");
        if ("DEVUELTO".equals(p.getEstado()))
            throw new IllegalStateException("El préstamo ya fue devuelto");

        String sql = "UPDATE prestamos SET estado = 'DEVUELTO', fecha_devolucion_real = CURRENT_DATE WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Agregar un libro a un préstamo existente (solo si está PENDIENTE) ─────

    public boolean agregarItem(int prestamoId, int libroId) throws Exception {
        Prestamo p = buscarPorId(prestamoId);
        if (p == null) throw new IllegalArgumentException("Préstamo no encontrado");
        if (!"PENDIENTE".equals(p.getEstado()))
            throw new IllegalStateException("Solo se pueden agregar libros a préstamos en estado PENDIENTE");

        String sql = "INSERT INTO prestamo_items(prestamo_id, libro_id) VALUES (?, ?)";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, prestamoId);
            ps.setInt(2, libroId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Eliminar un item de un préstamo (solo si está PENDIENTE) ─────────────

    public boolean eliminarItem(int prestamoId, int libroId) throws Exception {
        Prestamo p = buscarPorId(prestamoId);
        if (p == null) throw new IllegalArgumentException("Préstamo no encontrado");
        if (!"PENDIENTE".equals(p.getEstado()))
            throw new IllegalStateException("No se pueden quitar libros de un préstamo ya devuelto");

        String sql = "DELETE FROM prestamo_items WHERE prestamo_id = ? AND libro_id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, prestamoId);
            ps.setInt(2, libroId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Cancelar/eliminar préstamo completo (solo si está PENDIENTE) ──────────

    public boolean cancelarPrestamo(int id) throws Exception {
        Prestamo p = buscarPorId(id);
        if (p == null) throw new IllegalArgumentException("Préstamo no encontrado");
        if ("DEVUELTO".equals(p.getEstado()))
            throw new IllegalStateException("No se puede cancelar un préstamo ya devuelto");

        // Los items se eliminan en cascada por ON DELETE CASCADE
        String sql = "DELETE FROM prestamos WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}