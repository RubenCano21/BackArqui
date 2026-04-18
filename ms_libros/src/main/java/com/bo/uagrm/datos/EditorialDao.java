package com.bo.uagrm.datos;

import com.bo.uagrm.datos.entity.Editorial;
import com.bo.uagrm.datos.entity.Libro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class EditorialDao {

    private Editorial mapEditorial(ResultSet rs) throws Exception {
        Editorial editorial = new Editorial();
        editorial.setId(rs.getInt("id"));
        editorial.setNombre(rs.getString("nombre"));
        return editorial;
    }

    //Listar todos los editoriales
    public List<Editorial> listarEditoriales() throws Exception {
        List<Editorial> list = new ArrayList<>();
        String sql = "SELECT * FROM editoriales";

        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapEditorial(rs));
        }
        return list;
    }

    // ── Buscar libro por ID ───────────────────────────────────────────────────
    public Editorial buscarEditorialPorId(int id) throws Exception {
        String sql = "SELECT * FROM editoriales WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapEditorial(rs) : null;
            }
        }
    }

    // Registrar Editorial
    public boolean registrarEditorial(Editorial editorial) throws Exception {
        String sql = "INSERT INTO editoriales (nombre) VALUES (?)";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, editorial.getNombre());
            return ps.executeUpdate() > 0;
        }
    }

    // Actualizar Editorial
    public boolean actualizarEditorial(Editorial editorial) throws Exception {
        String sql = "UPDATE editoriales SET nombre = ? WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, editorial.getNombre());
            ps.setInt(2, editorial.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // Eliminar Editorial
    public boolean eliminarEditorial(int id) throws Exception {
        String sql = "DELETE FROM editoriales WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
