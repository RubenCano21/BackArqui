package com.bo.uagrm.negocio;

import com.bo.uagrm.datos.UsuarioDao;
import com.bo.uagrm.datos.entity.Usuario;
import com.bo.uagrm.negocio.dto.LoginRequest;
import com.bo.uagrm.negocio.dto.LoginResponse;

import java.util.List;

public class UsuarioN {

    private final UsuarioDao dao = new UsuarioDao();

    public List<Usuario> listar() throws Exception {
        return dao.listar();
    }

    public Usuario buscarPorId(Long id) throws Exception {
        return dao.buscarPorId(id);
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

        Usuario usuario = dao.buscarPorEmail(email);
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


    public boolean registrar(Usuario nuevo) throws Exception{
        return dao.registrar(nuevo);
    }

    public boolean actualizar(Usuario nuevo) throws Exception {
        return dao.actualizar(nuevo);
    }

    public boolean eliminar(Long id, Long solicitanteId) throws Exception {
        if (solicitanteId == null) {
            throw new IllegalArgumentException("Se requiere el header X-Usuario-Id para esta operacion");
        }
        return dao.eliminar(id, solicitanteId);
    }
}
