package com.bo.uagrm.datos;

import com.bo.uagrm.datos.entity.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDao {

    private Categoria mapCategoria(ResultSet rs) throws Exception {
        Categoria categoria = new Categoria();
        categoria.setId(rs.getInt("id"));
        categoria.setNombre(rs.getString("nombre"));
        return categoria;
    }

    public List<Categoria> listarCategorias() throws Exception {
        List<Categoria> list = new ArrayList<>();
        String sql = "SELECT * FROM categorias ORDER BY nombre";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapCategoria(rs));
        }
        return list;
    }

    public Categoria buscarCategoriaPorId(int id) throws Exception {
        String sql = "SELECT * FROM categorias WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapCategoria(rs) : null;
            }
        }
    }

    public boolean registrarCategoria(Categoria categoria) throws Exception {
        String sql = "INSERT INTO categorias (nombre) VALUES (?)";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoria.getNombre());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean actualizarCategoria(Categoria categoria) throws Exception {
        String sql = "UPDATE categorias SET nombre = ? WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoria.getNombre());
            ps.setInt(2, categoria.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminarCategoria(int id) throws Exception {
        String sql = "DELETE FROM categorias WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}

