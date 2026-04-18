package com.bo.uagrm.datos;

import com.bo.uagrm.datos.entity.Autor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AutorDao {

    private Autor mapAutor(ResultSet rs) throws Exception {
        Autor autor = new Autor();
        autor.setId(rs.getInt("id"));
        autor.setNombre(rs.getString("nombre"));
        autor.setApellido(rs.getString("apellido"));
        autor.setNacionalidad(rs.getString("nacionalidad"));
        return autor;
    }

    public List<Autor> listarAutores() throws Exception {
        List<Autor> list = new ArrayList<>();
        String sql = "SELECT * FROM autores ORDER BY apellido, nombre";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapAutor(rs));
        }
        return list;
    }

    public Autor buscarAutorPorId(int id) throws Exception {
        String sql = "SELECT * FROM autores WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapAutor(rs) : null;
            }
        }
    }

    public boolean registrarAutor(Autor autor) throws Exception {
        String sql = "INSERT INTO autores (nombre, apellido, nacionalidad) VALUES (?, ?, ?)";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, autor.getNombre());
            ps.setString(2, autor.getApellido());
            ps.setString(3, autor.getNacionalidad());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean actualizarAutor(Autor autor) throws Exception {
        String sql = "UPDATE autores SET nombre = ?, apellido = ?, nacionalidad = ? WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, autor.getNombre());
            ps.setString(2, autor.getApellido());
            ps.setString(3, autor.getNacionalidad());
            ps.setInt(4, autor.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminarAutor(int id) throws Exception {
        String sql = "DELETE FROM autores WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}

