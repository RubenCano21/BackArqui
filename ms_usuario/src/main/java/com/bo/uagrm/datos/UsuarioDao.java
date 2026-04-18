package com.bo.uagrm.datos;

import com.bo.uagrm.datos.entity.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDao {

    private Usuario mapUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("id"));
        usuario.setCi(rs.getInt("ci"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setApellido(rs.getString("apellido"));
        usuario.setEmail(rs.getString("email"));
        usuario.setTelefono(rs.getInt("telefono"));
        usuario.setFechaNac(rs.getDate("fecha_nac"));
        usuario.setGenero(rs.getString("genero"));
        usuario.setEstado(rs.getString("estado"));
        return usuario;
    }

    // Listar Usuarios
    public List<Usuario> listarUsuario() throws Exception {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios";

        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()){

            while (rs.next()) {
                lista.add(mapUsuario(rs));
            }
        }
        return lista;
    }

    // Buscar Usuario por ID
    public Usuario buscarUsuarioPorId(Long id) throws Exception {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUsuario(rs);
                }
            }
        }
        return null;
    }

    // Buscar Usuario por Email (case-insensitive)
    public Usuario buscarUsuarioPorEmail(String email) throws Exception {
        String sql = "SELECT * FROM usuarios WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUsuario(rs);
                }
            }
        }
        return null;
    }

    // Listar nombres de roles por usuario
    public List<String> obtenerRolesPorUsuarioId(Long usuarioId) throws Exception {
        List<String> roles = new ArrayList<>();
        String sql = "SELECT r.nombre " +
                "FROM roles r " +
                "INNER JOIN usuario_rol ur ON ur.rol_id = r.id " +
                "WHERE ur.usuario_id = ? " +
                "ORDER BY r.nombre";

        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roles.add(rs.getString("nombre"));
                }
            }
        }
        return roles;
    }

// Registrar Usuario y asignar rol ESTUDIANTE por defecto
    public boolean registrarUsuario(Usuario nuevo) throws Exception {
        String sqlUsuario = "INSERT INTO usuarios(ci, nombre, apellido, email, telefono, fecha_nac, genero, estado) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlRolId   = "SELECT id FROM roles WHERE UPPER(nombre) = 'ESTUDIANTE' LIMIT 1";
        String sqlRol     = "INSERT INTO usuario_rol(usuario_id, rol_id) VALUES (?, ?)";

        try (Connection conn = ConnectionDB.getConnection()) {
            // 1. Insertar usuario y recuperar el ID generado
            long nuevoId;
            try (PreparedStatement ps = conn.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, nuevo.getCi());
                ps.setString(2, nuevo.getNombre());
                ps.setString(3, nuevo.getApellido());
                ps.setString(4, nuevo.getEmail());
                ps.setInt(5, nuevo.getTelefono());
                ps.setDate(6, new java.sql.Date(nuevo.getFechaNac().getTime()));
                ps.setString(7, nuevo.getGenero());
                ps.setString(8, nuevo.getEstado());
                int rows = ps.executeUpdate();
                if (rows == 0) return false;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) return false;
                    nuevoId = keys.getLong(1);
                }
            }

            // 2. Obtener el ID del rol ESTUDIANTE
            long rolId;
            try (PreparedStatement ps = conn.prepareStatement(sqlRolId);
                 ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return true; // rol no existe en BD, igual el usuario fue creado
                rolId = rs.getLong(1);
            }

            // 3. Asignar rol ESTUDIANTE al nuevo usuario
            try (PreparedStatement ps = conn.prepareStatement(sqlRol)) {
                ps.setLong(1, nuevoId);
                ps.setLong(2, rolId);
                ps.executeUpdate();
            }

            return true;
        }
    }

    // Actualizar Usuario
    public boolean actualizarUsuario(Usuario usuario) throws Exception {
        String sql = "UPDATE usuarios SET ci = ?, nombre = ?, apellido = ?, email = ?, " +
                "telefono = ?, fecha_nac = ?, genero = ?, estado = ? WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setInt(1, usuario.getCi());
            ps.setString(2, usuario.getNombre());
            ps.setString(3, usuario.getApellido());
            ps.setString(4, usuario.getEmail());
            ps.setInt(5, usuario.getTelefono());
            ps.setDate(6, new java.sql.Date(usuario.getFechaNac().getTime()));
            ps.setString(7, usuario.getGenero());
            ps.setString(8, usuario.getEstado());
            ps.setLong(9, usuario.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // Verificar si un usuario tiene rol ADMIN
    public boolean tieneRolAdmin(Long usuarioId) throws Exception {
        String sql = "SELECT 1 FROM usuario_rol ur " +
                "INNER JOIN roles r ON r.id = ur.rol_id " +
                "WHERE ur.usuario_id = ? AND UPPER(r.nombre) = 'ADMIN'";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Eliminar Usuario - la validación de rol ADMIN la realiza el API Gateway de forma centralizada
    public boolean eliminarUsuario(Long id) throws Exception {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection conn = ConnectionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }


}
