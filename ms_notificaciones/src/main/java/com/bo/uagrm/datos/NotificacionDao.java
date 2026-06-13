package com.bo.uagrm.datos;

import com.bo.uagrm.datos.entity.Notificacion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificacionDao {

    // ── Mapeo desde ResultSet ─────────────────────────────────────────────────
    private Notificacion map(ResultSet rs) throws SQLException {
        Notificacion n = new Notificacion();
        n.setId(rs.getInt("id"));
        n.setUsuarioId(rs.getInt("usuario_id"));

        int libroId = rs.getInt("libro_id");
        n.setLibroId(rs.wasNull() ? null : libroId);

        n.setTipo(rs.getString("tipo"));
        n.setMensaje(rs.getString("mensaje"));
        n.setCanal(rs.getString("canal"));
        n.setEstado(rs.getString("estado"));
        n.setEmailDestino(rs.getString("email_destino"));

        Timestamp creada = rs.getTimestamp("fecha_creada");
        n.setFechaCreada(creada != null ? creada.toLocalDateTime() : null);

        Timestamp enviada = rs.getTimestamp("fecha_enviada");
        n.setFechaEnviada(enviada != null ? enviada.toLocalDateTime() : null);

        return n;
    }

    // ── Registrar una notificación nueva (estado PENDIENTE) ───────────────────
    public int registrar(Notificacion n) throws Exception {
        String sql = """
            INSERT INTO notificaciones
                (usuario_id, libro_id, tipo, mensaje, canal, estado, email_destino, fecha_creada)
            VALUES (?, ?, ?, ?, ?, 'PENDIENTE', ?, NOW())
            RETURNING id
            """;
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, n.getUsuarioId());
            if (n.getLibroId() != null) ps.setInt(2, n.getLibroId());
            else                         ps.setNull(2, Types.INTEGER);
            ps.setString(3, n.getTipo());
            ps.setString(4, n.getMensaje());
            ps.setString(5, n.getCanal());
            ps.setString(6, n.getEmailDestino());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        }
    }

    // ── Marcar como ENVIADA ───────────────────────────────────────────────────
    public boolean marcarEnviada(int id) throws Exception {
        String sql = """
            UPDATE notificaciones
            SET estado = 'ENVIADA', fecha_enviada = NOW()
            WHERE id = ?
            """;
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Marcar como FALLIDA ───────────────────────────────────────────────────
    public boolean marcarFallida(int id) throws Exception {
        String sql = "UPDATE notificaciones SET estado = 'FALLIDA' WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Listar todas (para el admin) ──────────────────────────────────────────
    public List<Notificacion> listarTodas() throws Exception {
        List<Notificacion> lista = new ArrayList<>();
        String sql = "SELECT * FROM notificaciones ORDER BY fecha_creada DESC";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(map(rs));
        }
        return lista;
    }

    // ── Listar por usuario ────────────────────────────────────────────────────
    public List<Notificacion> listarPorUsuario(int usuarioId) throws Exception {
        List<Notificacion> lista = new ArrayList<>();
        String sql = """
            SELECT * FROM notificaciones
            WHERE usuario_id = ?
            ORDER BY fecha_creada DESC
            """;
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(map(rs));
            }
        }
        return lista;
    }

    // ── Buscar por ID ─────────────────────────────────────────────────────────
    public Notificacion buscarPorId(int id) throws Exception {
        String sql = "SELECT * FROM notificaciones WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // ── Listar PENDIENTES (para reintento automático) ─────────────────────────
    public List<Notificacion> listarPendientes() throws Exception {
        List<Notificacion> lista = new ArrayList<>();
        String sql = """
            SELECT * FROM notificaciones
            WHERE estado = 'PENDIENTE'
            ORDER BY fecha_creada ASC
            """;
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(map(rs));
        }
        return lista;
    }
}