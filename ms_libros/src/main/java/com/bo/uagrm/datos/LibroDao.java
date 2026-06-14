package com.bo.uagrm.datos;

import com.bo.uagrm.datos.entity.Autor;
import com.bo.uagrm.datos.entity.Libro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LibroDao {

    // ── Mapeo limpio desde la tabla libros ────────────────────────────────────
    private Libro mapLibro(ResultSet rs) throws SQLException {
        Libro libro = new Libro();
        libro.setId(rs.getInt("id"));
        libro.setCodigo(rs.getInt("codigo"));
        libro.setTitulo(rs.getString("titulo"));
        libro.setAnio(rs.getInt("anio"));
        libro.setEdicion(rs.getString("edicion"));
        libro.setNroEjemplar(rs.getInt("nro_ejemplar"));
        int catId = rs.getInt("categoria_id");
        libro.setCategoriaId(rs.wasNull() ? null : catId);
        int editId = rs.getInt("editorial_id");
        libro.setEditorialId(rs.wasNull() ? null : editId);
        int autId = rs.getInt("autor_id");
        libro.setAutorId(rs.wasNull() ? null : autId);
        return libro;
    }

    // ── Listar todos los libros ───────────────────────────────────────────────
    public List<Libro> listarLibros() throws Exception {
        List<Libro> lista = new ArrayList<>();
        String sql = "SELECT * FROM libros ORDER BY id";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapLibro(rs));
        }
        return lista;
    }

    // ── Buscar libro por ID ───────────────────────────────────────────────────
    public Libro buscarLibroPorId(int id) throws Exception {
        String sql = "SELECT * FROM libros WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapLibro(rs) : null;
            }
        }
    }

    // ── Consulta separada: nombre de la categoría de un libro ────────────────
    public String buscarNombreCategoria(int categoriaId) throws Exception {
        String sql = "SELECT nombre FROM categorias WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoriaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("nombre") : null;
            }
        }
    }

    // ── Consulta separada: nombre de la editorial de un libro ─────────────────
    public String buscarNombreEditorial(int editorialId) throws Exception {
        String sql = "SELECT nombre FROM editoriales WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, editorialId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("nombre") : null;
            }
        }
    }

    // ── Consulta separada: autores de un libro (via libro_autor) ─────────────
    public List<Autor> buscarAutoresPorLibro(int libroId) throws Exception {
        List<Autor> autores = new ArrayList<>();
        String sql = "SELECT a.* FROM autores a " +
                     "INNER JOIN libro_autor la ON la.autor_id = a.id " +
                     "WHERE la.libro_id = ? ORDER BY a.apellido";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, libroId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Autor a = new Autor();
                    a.setId(rs.getInt("id"));
                    a.setNombre(rs.getString("nombre"));
                    a.setApellido(rs.getString("apellido"));
                    a.setNacionalidad(rs.getString("nacionalidad"));
                    autores.add(a);
                }
            }
        }
        return autores;
    }

    // ── Registrar Libro ───────────────────────────────────────────────────────
    public boolean registrarLibro(Libro nuevo) throws Exception {
        String sql = "INSERT INTO libros(codigo, titulo, anio, edicion, nro_ejemplar, " +
                        "categoria_id, editorial_id, autor_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nuevo.getCodigo());
            ps.setString(2, nuevo.getTitulo());
            ps.setInt(3, nuevo.getAnio());
            ps.setString(4, nuevo.getEdicion());
            ps.setInt(5, nuevo.getNroEjemplar());
            if (nuevo.getCategoriaId() != null) ps.setInt(6, nuevo.getCategoriaId());
            else ps.setNull(6, java.sql.Types.INTEGER);
            if (nuevo.getEditorialId() != null) ps.setInt(7, nuevo.getEditorialId());
            else ps.setNull(7, java.sql.Types.INTEGER);
            if (nuevo.getAutorId() != null) ps.setInt(8, nuevo.getAutorId());
            else ps.setNull(8, java.sql.Types.INTEGER);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Actualizar Libro ──────────────────────────────────────────────────────
    public boolean actualizarLibro(Libro libro) throws Exception {
        String sql = "UPDATE libros SET titulo = ?, anio = ?, edicion = ?, " +
                     "nro_ejemplar = ?, categoria_id = ?, editorial_id = ?, autor_id = ? WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, libro.getTitulo());
            ps.setInt(2, libro.getAnio());
            ps.setString(3, libro.getEdicion());
            ps.setInt(4, libro.getNroEjemplar());
            if (libro.getCategoriaId() != null) ps.setInt(5, libro.getCategoriaId());
            else ps.setNull(5, java.sql.Types.INTEGER);
            if (libro.getEditorialId() != null) ps.setInt(6, libro.getEditorialId());
            else ps.setNull(6, java.sql.Types.INTEGER);
            if (libro.getAutorId() != null) ps.setInt(7, libro.getAutorId());
            else ps.setNull(7, java.sql.Types.INTEGER);
            ps.setInt(8, libro.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // ── Actualizar cantidad de ejemplares disponibles (delta: +1 devolucion, -1 prestamo) ──
    public boolean actualizarEjemplares(int id, int delta) throws Exception {
        String sql = "UPDATE libros SET nro_ejemplar = nro_ejemplar + ? " +
                     "WHERE id = ? AND nro_ejemplar + ? >= 0";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, id);
            ps.setInt(3, delta);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Eliminar Libro ────────────────────────────────────────────────────────
    public boolean eliminarLibro(int id) throws Exception {
        String sql = "DELETE FROM libros WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
