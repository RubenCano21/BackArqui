package com.bo.uagrm.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;

/**
 * Configuración centralizada de Jackson compartida por todos los microservicios.
 * Importar desde: com.bo.uagrm.commons.JsonConfig
 */
public class JsonConfig {

    @Getter
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}

