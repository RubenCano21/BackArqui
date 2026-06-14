package com.bo.uagrm.datos;

import com.bo.uagrm.datos.entity.ListaEspera;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ListaEsperaDao {

    private ListaEspera mapListaEspera(ResultSet rs) throws SQLException {
        ListaEspera listaEspera = new ListaEspera();
        listaEspera.setId(rs.getInt("id"));
        listaEspera.setIdLibro(rs.getInt("libro_id"));
        listaEspera.setIdUsuario(rs.getInt("usuario_id"));
        listaEspera.setEmailUsuario(rs.getString("email_usuario"));
        listaEspera.setEstado(rs.getString("estado"));
        return listaEspera;
    }

    public boolean suscribir(int libroId, int usuarioId, String emailUsuario) throws Exception {
        String sql = """
            INSERT INTO lista_espera (libro_id, usuario_id, email_usuario, estado)
            VALUES (?, ?, ?, 'ACTIVO')
            ON CONFLICT (libro_id, usuario_id) DO UPDATE
                SET estado         = 'ACTIVO',
                    email_usuario  = EXCLUDED.email_usuario,
                    fecha_registro = NOW()
            WHERE lista_espera.estado <> 'ACTIVO'
            """;
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, libroId);
            ps.setInt(2, usuarioId);
            ps.setString(3, emailUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    public List<ListaEspera> listarActivosPorLibro(int libroId) throws Exception {
        List<ListaEspera> lista = new ArrayList<>();
        String sql = """
            SELECT * FROM lista_espera
            WHERE libro_id = ? AND estado = 'ACTIVO'
            ORDER BY fecha_registro ASC
            """;
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, libroId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapListaEspera(rs));
            }
        }
        return lista;
    }

    public boolean marcarNotificados(int libroId) throws Exception {
        String sql = """
            UPDATE lista_espera
            SET estado = 'NOTIFICADO', fecha_notificado = NOW()
            WHERE libro_id = ? AND estado = 'ACTIVO'
            """;
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, libroId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean cancelarSuscripcion(int libroId, int usuarioId) throws Exception {
        String sql = """
            UPDATE lista_espera SET estado = 'CANCELADO'
            WHERE libro_id = ? AND usuario_id = ? AND estado = 'ACTIVO'
            """;
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, libroId);
            ps.setInt(2, usuarioId);
            return ps.executeUpdate() > 0;
        }
    }
}