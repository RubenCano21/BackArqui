package com.bo.uagrm.datos;

import com.bo.uagrm.datos.entity.Roles;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RolesDao {

    private Roles map(ResultSet rs) throws Exception {
        return new Roles(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getInt("dias_gracia"),
                rs.getBigDecimal("monto_dia"),
                rs.getBoolean("puede_prestar")
        );
    }

    public List<Roles> listarTodos() throws Exception {
        List<Roles> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, dias_gracia, monto_dia, puede_prestar FROM roles ORDER BY id";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(map(rs));
        }
        return lista;
    }

    /** Obtiene el rol completo (con política de multa) de un usuario. */
    public Roles obtenerRolPorUsuarioId(long usuarioId) throws Exception {
        String sql = """
            SELECT r.id, r.nombre, r.dias_gracia, r.monto_dia, r.puede_prestar
            FROM roles r
            INNER JOIN usuario_rol ur ON ur.rol_id = r.id
            WHERE ur.usuario_id = ?
            LIMIT 1
            """;
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Lista solo los nombres de roles — endpoint GET /usuarios/{id}/roles */
    public List<String> obtenerNombresPorUsuarioId(long usuarioId) throws Exception {
        List<String> nombres = new ArrayList<>();
        String sql = """
            SELECT r.nombre FROM roles r
            INNER JOIN usuario_rol ur ON ur.rol_id = r.id
            WHERE ur.usuario_id = ?
            ORDER BY r.nombre
            """;
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) nombres.add(rs.getString("nombre"));
            }
        }
        return nombres;
    }


}
