package com.bo.uagrm.negocio;

import com.bo.uagrm.datos.UsuarioDao;
import com.bo.uagrm.datos.entity.Usuario;
import com.bo.uagrm.negocio.dto.LoginRequest;
import com.bo.uagrm.negocio.dto.LoginResponse;

import java.util.List;

public class UsuarioN {

    private final UsuarioDao dao = new UsuarioDao();

    public List<Usuario> listarUsuarios() throws Exception {
        return dao.listarUsuario();
    }

    public Usuario buscarPorId(Long id) throws Exception {
        return dao.buscarUsuarioPorId(id);
    }

    public LoginResponse login(LoginRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Debe enviar las credenciales");
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String password = request.getPassword() == null ? "" : request.getPassword().trim();

        if (email.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Email y password son obligatorios");
        }

        Usuario usuario = dao.buscarUsuarioPorEmail(email);
        if (usuario == null) {
            throw new AuthException("Credenciales invalidas");
        }

        if (!String.valueOf(usuario.getCi()).equals(password)) {
            throw new AuthException("Credenciales invalidas");
        }

        LoginResponse response = new LoginResponse();
        response.setAutenticado(true);
        response.setMensaje("Login exitoso");
        response.setUsuario(new LoginResponse.UsuarioData(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getEstado()
        ));
        response.setRoles(dao.obtenerRolesPorUsuarioId(usuario.getId()));
        return response;
    }


    public boolean registrarUsuario(Usuario nuevo) throws Exception{
        return dao.registrarUsuario(nuevo);
    }

    public boolean actualizarUsuario(Usuario nuevo) throws Exception {
        return dao.actualizarUsuario(nuevo);
    }

    public List<String> obtenerRoles(Long id) throws Exception {
        return dao.obtenerRolesPorUsuarioId(id);
    }

    public boolean eliminarUsuario(Long id) throws Exception {
        return dao.eliminarUsuario(id);
    }
}
