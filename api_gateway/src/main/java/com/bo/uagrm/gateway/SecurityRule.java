package com.bo.uagrm.gateway;

/**
 * Regla de seguridad del API Gateway.
 *
 * Define qué combinación de path (regex) + método HTTP requiere
 * que el usuario tenga un rol específico.
 *
 * Ejemplo:
 *   new SecurityRule("/api/usuarios/\\d+", "DELETE", "ADMIN")
 *   → Solo usuarios con rol ADMIN pueden hacer DELETE en /api/usuarios/{id}
 */
public class SecurityRule {

    /** Regex que se aplica sobre el path de la petición entrante. */
    private final String pathRegex;

    /** Método HTTP requerido, ej: "DELETE", "POST". Vacío = cualquier método. */
    private final String method;

    /** Rol necesario para acceder, ej: "ADMIN". */
    private final String requiredRole;

    public SecurityRule(String pathRegex, String method, String requiredRole) {
        this.pathRegex    = pathRegex;
        this.method       = method.toUpperCase();
        this.requiredRole = requiredRole.toUpperCase();
    }

    /**
     * @return true si esta regla aplica sobre la petición dada (path + método)
     */
    public boolean matches(String path, String method) {
        boolean pathMatch   = path.matches(this.pathRegex);
        boolean methodMatch = this.method.isEmpty() || this.method.equals(method.toUpperCase());
        return pathMatch && methodMatch;
    }

    public String getRequiredRole() {
        return requiredRole;
    }
}

