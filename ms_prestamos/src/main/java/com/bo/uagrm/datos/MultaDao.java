package com.bo.uagrm.datos;

import com.bo.uagrm.datos.entity.Multa;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MultaDao {

    private Multa map(ResultSet rs) throws SQLException {
        Multa m = new Multa();
        m.setId(rs.getInt("id"));
        m.setPrestamoId(rs.getInt("prestamo_id"));
        m.setTipoCalculo(rs.getString("tipo_calculo"));
        m.setDiasRetraso(rs.getInt("dias_retraso"));
        m.setDiasGraciaAplicados(rs.getInt("dias_gracia_aplicados"));
        m.setMontoPorDia(rs.getBigDecimal("monto_por_dia"));
        m.setMontoCalculado(rs.getBigDecimal("monto_calculado"));
        m.setEstado(rs.getString("estado"));
        m.setFechaCreacion(rs.getDate("fecha_creacion"));
        m.setFechaPago(rs.getDate("fecha_pago"));
        return m;
    }

    /** Registra la multa y retorna el ID generado. */
    public int registrar(Multa multa) throws Exception {
        String sql = """
            INSERT INTO multas
              (prestamo_id, tipo_calculo, dias_retraso, dias_gracia_ap,
               monto_por_dia, monto_calculado, estado)
            VALUES (?, ?, ?, ?, ?, ?, 'PENDIENTE')
            RETURNING id
            """;
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, multa.getPrestamoId());
            ps.setString(2, multa.getTipoCalculo());
            ps.setInt(3, multa.getDiasRetraso());
            ps.setInt(4, multa.getDiasGraciaAplicados());
            ps.setBigDecimal(5, multa.getMontoPorDia());
            ps.setBigDecimal(6, multa.getMontoCalculado());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        }
    }

    public boolean marcarPagada(int id) throws Exception {
        String sql = "UPDATE multas SET estado = 'PAGADA', fecha_pago = CURRENT_DATE WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Multa> listarPorUsuario(int usuarioId) throws Exception {
        List<Multa> lista = new ArrayList<>();
        // Join con prestamos para filtrar por estudiante_id
        String sql = """
            SELECT m.* FROM multas m
            INNER JOIN prestamos p ON p.id = m.prestamo_id
            WHERE p.usuario_id = ?
            ORDER BY m.fecha_creacion DESC
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
}