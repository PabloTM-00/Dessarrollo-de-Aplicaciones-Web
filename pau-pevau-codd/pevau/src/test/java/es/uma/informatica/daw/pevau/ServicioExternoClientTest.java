package es.uma.informatica.daw.pevau;

import es.uma.informatica.daw.pevau.servicios.ServicioExternoClient;
import es.uma.informatica.daw.pevau.dtos.Materia;
import es.uma.informatica.daw.pevau.excepciones.ConvocatoriaActualNoEncontradaException;
import es.uma.informatica.daw.pevau.excepciones.ServicioExternoException;
import es.uma.informatica.daw.pevau.excepciones.MateriaNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import java.util.Collections;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletionException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

@SpringBootTest
@DisplayName("Pruebas para ServicioExternoClient")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ServicioExternoClientTest {

    @Autowired
    private ServicioExternoClient servicioExternoClient;

    private MockRestServiceServer mockServer;

    @Value("${external.token}")
    private String expectedToken;

    /**
     * Configura el servidor de simulación vinculándolo al RestClient interno del servicio.
     * Se usa un método auxiliar en lugar de @BeforeEach global para no interferir 
     * con las pruebas marcadas como @Tag("integration").
     */
    private void prepareMockServer(boolean ignoreOrder) {
        RestClient internalClient = (RestClient) ReflectionTestUtils.getField(servicioExternoClient, "client");
        RestClient.Builder builder = internalClient.mutate();
        
        if (ignoreOrder) {
            mockServer = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        } else {
            mockServer = MockRestServiceServer.bindTo(builder).build();
        }
        
        ReflectionTestUtils.setField(servicioExternoClient, "client", builder.build());
    }

    @Test
    @DisplayName("obtenerConvocatoriaActual sends correct token in Authorization header")
    void obtenerConvocatoriaActual_ShouldSendCorrectToken() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/convocatorias/actual"))
                .andExpect(header("Authorization", "Bearer " + expectedToken))
                .andRespond(withSuccess("{\"idConvocatoria\": 1, \"nombre\": \"Junio 2024\"}", MediaType.APPLICATION_JSON));

        // Act
        servicioExternoClient.obtenerConvocatoriaActual();

        // Assert
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerConvocatoriaActual throws ConvocatoriaActualNoEncontradaException on 404")
    void obtenerConvocatoriaActual_ShouldThrowExceptionWhenNotFound() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/convocatorias/actual"))
                .andRespond(withResourceNotFound());

        // Act & Assert
        assertThrows(ConvocatoriaActualNoEncontradaException.class, () -> 
            servicioExternoClient.obtenerConvocatoriaActual()
        );

        // Assert
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerConvocatoriaActual throws ConvocatoriaActualNoEncontradaException when response body is null")
    void obtenerConvocatoriaActual_ShouldThrowExceptionWhenResultIsNull() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/convocatorias/actual"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON)); // Simula cuerpo nulo

        // Act & Assert
        assertThrows(ConvocatoriaActualNoEncontradaException.class, () -> 
            servicioExternoClient.obtenerConvocatoriaActual()
        );
    }

    @Test
    @DisplayName("obtenerConvocatoriaActual throws ServicioExternoException on 5xx server error")
    void obtenerConvocatoriaActual_ShouldThrowServicioExternoExceptionWhenServerError() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/convocatorias/actual"))
                .andRespond(withServerError());

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
            servicioExternoClient.obtenerConvocatoriaActual()
        );

        // Assert
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerConvocatoriaActual throws ServicioExternoException on network/timeout error")
    void obtenerConvocatoriaActual_ShouldThrowServicioExternoExceptionWhenNetworkError() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/convocatorias/actual"))
                .andRespond(request -> { throw new java.io.IOException("Timeout simulado"); });

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
            servicioExternoClient.obtenerConvocatoriaActual()
        );

        // Assert
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerConvocatoriaActual throws ServicioExternoException on 4xx client error")
    void obtenerConvocatoriaActual_ShouldThrowServicioExternoExceptionWhenClientError() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/convocatorias/actual"))
                .andRespond(withBadRequest());

        // Act & Assert
        assertThrows(ServicioExternoException.class, () ->
            servicioExternoClient.obtenerConvocatoriaActual()
        );
        // Assert
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerMateria includes the correct Authorization header")
    void obtenerMateria_ShouldSendCorrectAuthorizationHeader() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/501"))
                .andExpect(header("Authorization", "Bearer " + expectedToken))
                .andRespond(withSuccess("{\"id\": 501, \"nombre\": \"Física\", \"eliminada\": false}", MediaType.APPLICATION_JSON));

        // Act
        servicioExternoClient.obtenerMateria(501L);

        // Assert
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerMateria requests the resource with the correct ID in the URL")
    void obtenerMateria_ShouldRequestCorrectResourceUrl() {
        // Arrange
        prepareMockServer(false);
        Long idMateria = 501L;

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/" + idMateria))
                .andRespond(withSuccess("{\"id\": 501, \"nombre\": \"Física\", \"eliminada\": false}", MediaType.APPLICATION_JSON));

        // Act
        servicioExternoClient.obtenerMateria(idMateria);

        // Assert
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerMateria correctly maps response body to Materia DTO fields")
    void obtenerMateria_ShouldMapResponseToDtoCorrectly() {
        // Arrange
        prepareMockServer(false);
        Long idMateria = 501L;
        String jsonResponse = "{\"id\": 501, \"nombre\": \"Física\", \"eliminada\": false}";

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/" + idMateria))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // Act
        Materia result = servicioExternoClient.obtenerMateria(idMateria);

        // Assert
        assertNotNull(result);
        assertEquals(idMateria, result.id());
        assertEquals("Física", result.nombre());
        assertFalse(result.eliminada());
    }

    @Test
    @DisplayName("obtenerMateria throws MateriaNotFoundException when response body is null")
    void obtenerMateria_ShouldThrowExceptionWhenResultIsNull() {
        // Arrange
        prepareMockServer(false);
        Long idMateria = 501L;

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/" + idMateria))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThrows(MateriaNotFoundException.class, () -> 
            servicioExternoClient.obtenerMateria(idMateria)
        );
    }

    @Test
    @DisplayName("obtenerMateria throws MateriaNotFoundException on 404")
    void obtenerMateria_ShouldThrowExceptionWhenNotFound() {
        // Arrange
        prepareMockServer(false);

        Long idMateria = 999L;
        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/" + idMateria))
                .andRespond(withResourceNotFound());

        // Act & Assert
        assertThrows(MateriaNotFoundException.class, () -> 
            servicioExternoClient.obtenerMateria(idMateria)
        );

        // Assert
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerMateria throws ServicioExternoException on 5xx server error")
    void obtenerMateria_ShouldThrowServicioExternoExceptionWhenServerError() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/501"))
                .andRespond(withServerError());

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
            servicioExternoClient.obtenerMateria(501L)
        );

        // Assert
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerMateria throws exception on 400 Bad Request")
    void obtenerMateria_ShouldThrowExceptionOnBadRequest() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/501"))
                .andRespond(withBadRequest());

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> servicioExternoClient.obtenerMateria(501L));
    }

    @Test
    @DisplayName("obtenerMateria throws ServicioExternoException on network error")
    void obtenerMateria_ShouldThrowServicioExternoExceptionWhenNetworkError() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/501"))
                .andRespond(request -> { throw new java.io.IOException("Error de red"); });

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
            servicioExternoClient.obtenerMateria(501L)
        );

        // Assert
        mockServer.verify();
    }

    @Test
    @Tag("integration")
    @DisplayName("Integration: obtenerConvocatoriaActual returns a valid ID from the real service")
    void obtenerConvocatoriaActual_IntegrationTest() {
        // Act
        Long result = servicioExternoClient.obtenerConvocatoriaActual();

        // Assert
        assertNotNull(result, "The real service should return a valid convocatoria ID");
        assertTrue(result > 0, "The ID should be a positive number");
    }

    @Test
    @Tag("integration")
    @DisplayName("Integration: obtenerMateria returns a valid Materia from the real service")
    void obtenerMateria_IntegrationTest() {
        // Arrange
        Long idMateria = 1L;

        // Act
        Materia result = servicioExternoClient.obtenerMateria(idMateria);

        // Assert
        assertNotNull(result, "El objeto Materia no debería ser null");
        assertEquals(idMateria, result.id(), "El ID devuelto no coincide con el solicitado");
        assertNotNull(result.nombre(), "El campo 'nombre' no debería ser null");
        assertFalse(result.nombre().isBlank(), "El campo 'nombre' no debería estar vacío");
        // Verificamos que 'eliminada' es accesible (al ser un boolean primitivo o Booleano, 
        // el simple hecho de que el test no lance una excepción de mapeo es buena señal).
    }

    @Test
    @DisplayName("obtenerTodasMaterias returns list of Materias on success")
    void obtenerTodasMaterias_ShouldReturnListOnSuccess() {
        // Arrange
        prepareMockServer(false);
        String jsonResponse = "[{\"id\": 1, \"nombre\": \"M1\", \"eliminada\": false}, {\"id\": 2, \"nombre\": \"M2\", \"eliminada\": true}]";

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // Act
        List<Materia> result = servicioExternoClient.obtenerTodasMaterias();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerTodasMaterias returns empty list when response body is null")
    void obtenerTodasMaterias_ShouldReturnEmptyListWhenResultIsNull() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        // Act
        List<Materia> result = servicioExternoClient.obtenerTodasMaterias();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("obtenerTodasMaterias throws ServicioExternoException on 5xx server error")
    void obtenerTodasMaterias_ShouldThrowServicioExternoExceptionWhenServerError() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias"))
                .andRespond(withServerError());

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
            servicioExternoClient.obtenerTodasMaterias()
        );
    }

    @Test
    @DisplayName("obtenerTodasMaterias throws ServicioExternoException on network error")
    void obtenerTodasMaterias_ShouldThrowServicioExternoExceptionWhenNetworkError() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias"))
                .andRespond(request -> { throw new java.io.IOException("Error de red"); });

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
            servicioExternoClient.obtenerTodasMaterias()
        );
    }

    @Test
    @DisplayName("obtenerMateriasBatch returns multiple Materias in parallel")
    void obtenerMateriasBatch_ShouldReturnMateriasInParallelWhenIdsProvided() {
        // Arrange
        // Usamos ignoreOrder=true porque las llamadas paralelas no tienen orden garantizado
        prepareMockServer(true);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/1"))
                .andRespond(withSuccess("{\"id\": 1, \"nombre\": \"M1\", \"eliminada\": false}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/2"))
                .andRespond(withSuccess("{\"id\": 2, \"nombre\": \"M2\", \"eliminada\": false}", MediaType.APPLICATION_JSON));

        // Act
        List<Materia> result = servicioExternoClient.obtenerMateriasBatch(Set.of(1L, 2L));

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerMateriasBatch returns empty list when no IDs are provided")
    void obtenerMateriasBatch_ShouldReturnEmptyListWhenIdsProvidedIsEmpty() {
        // Act
        List<Materia> result = servicioExternoClient.obtenerMateriasBatch(Collections.emptySet());
        
        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("obtenerMateriasBatch throws MateriaNotFoundException if any ID is not found")
    void obtenerMateriasBatch_ShouldThrowExceptionWhenOneMateriaNotFound() {
        // Arrange
        prepareMockServer(true);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/1"))
                .andRespond(withSuccess("{\"id\": 1, \"nombre\": \"M1\", \"eliminada\": false}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/999"))
                .andRespond(withResourceNotFound());

        // Act & Assert
        assertThrows(MateriaNotFoundException.class, () -> 
            servicioExternoClient.obtenerMateriasBatch(Set.of(1L, 999L))
        );
    }

    @Test
    @DisplayName("obtenerMateriasBatch rethrows CompletionException if cause is not a RuntimeException")
    void obtenerMateriasBatch_ShouldThrowOriginalCompletionExceptionWhenCauseIsNotRuntimeException() {
        // Arrange
        prepareMockServer(true);
        
        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/materias/1"))
                .andRespond(request -> { throw new Error("Trigger non-runtime exception"); });

        // Act & Assert
        assertThrows(CompletionException.class, () -> 
            servicioExternoClient.obtenerMateriasBatch(Set.of(1L))
        );
    }

    @Test
    @Tag("integration")
    @DisplayName("Integration: obtenerMateriasBatch returns Materias from the real service")
    void obtenerMateriasBatch_IntegrationTest() {
        // Arrange
        Set<Long> ids = Set.of(1L);
        
        // Act
        List<Materia> result = servicioExternoClient.obtenerMateriasBatch(ids);

        // Assert
        assertNotNull(result, "La lista no debería ser null");
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    @DisplayName("obtenerResponsableSede returns idUsuario on successful response")
    void obtenerResponsableSede_ShouldReturnIdUsuarioOnSuccess() {
        // Arrange
        prepareMockServer(false);

        Long idSede = 1L;
        String jsonResponse = "{\"responsable\": {\"idUsuario\": 555}}";

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/sedes/" + idSede))
                .andExpect(header("Authorization", "Bearer " + expectedToken))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // Act
        Long result = servicioExternoClient.obtenerResponsableSede(idSede);

        // Assert
        assertEquals(555L, result);
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerResponsableSede throws ServicioExternoException when response body is null")
    void obtenerResponsableSede_ShouldThrowExceptionWhenResultIsNull() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/sedes/1"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
                servicioExternoClient.obtenerResponsableSede(1L));
    }

    @Test
    @DisplayName("obtenerResponsableSede throws ServicioExternoException on 404")
    void obtenerResponsableSede_ShouldThrowExceptionWhenNotFound() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/sedes/1"))
                .andRespond(withResourceNotFound());

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
                servicioExternoClient.obtenerResponsableSede(1L));
    }

    @Test
    @DisplayName("obtenerResponsableSede throws ServicioExternoException on 5xx server error")
    void obtenerResponsableSede_ShouldThrowExceptionWhenServerError() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/sedes/1"))
                .andRespond(withServerError());

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
                servicioExternoClient.obtenerResponsableSede(1L));
    }

    @Test
    @DisplayName("obtenerResponsableSede throws ServicioExternoException when 'responsable' key is missing")
    void obtenerResponsableSede_ShouldThrowExceptionWhenResponsableKeyIsMissing() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/sedes/1"))
                .andRespond(withSuccess("{\"id\": 1}", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
                servicioExternoClient.obtenerResponsableSede(1L));
    }

    @Test
    @DisplayName("obtenerResponsableSede throws ServicioExternoException when 'responsable' value is null")
    void obtenerResponsableSede_ShouldThrowExceptionWhenResponsableIsNull() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/sedes/1"))
                .andRespond(withSuccess("{\"responsable\": null}", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
                servicioExternoClient.obtenerResponsableSede(1L));
    }

    @Test
    @DisplayName("obtenerResponsableSede throws ServicioExternoException when 'idUsuario' is missing")
    void obtenerResponsableSede_ShouldThrowExceptionWhenIdUsuarioIsMissing() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/sedes/1"))
                .andRespond(withSuccess("{\"responsable\": {\"dni\": \"123\"}}", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
                servicioExternoClient.obtenerResponsableSede(1L));
    }

    @Test
    @DisplayName("obtenerResponsableSede throws exception when 'idUsuario' value is null in JSON")
    void obtenerResponsableSede_ShouldThrowExceptionWhenIdUsuarioIsNull() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/sedes/1"))
                .andRespond(withSuccess("{\"responsable\": {\"idUsuario\": null}}", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> servicioExternoClient.obtenerResponsableSede(1L));
    }

    @Test
    @DisplayName("obtenerResponsableSede throws exception when 'idUsuario' is not a numeric type")
    void obtenerResponsableSede_ShouldThrowExceptionWhenIdUsuarioIsNotANumber() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/sedes/1"))
                .andRespond(withSuccess("{\"responsable\": {\"idUsuario\": \"string_id\"}}", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> servicioExternoClient.obtenerResponsableSede(1L));
    }

    @Test
    @DisplayName("obtenerResponsableSede throws exception when 'responsable' is not a JSON object")
    void obtenerResponsableSede_ShouldThrowExceptionWhenResponsableIsNotAnObject() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/sedes/1"))
                .andRespond(withSuccess("{\"responsable\": 123}", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> servicioExternoClient.obtenerResponsableSede(1L));
    }

    @Test
    @DisplayName("obtenerResponsableSede throws ServicioExternoException on 4xx client error")
    void obtenerResponsableSede_ShouldThrowServicioExternoExceptionWhenClientError() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/sedes/1"))
                .andRespond(withBadRequest());

        // Act & Assert
        assertThrows(ServicioExternoException.class, () ->
                servicioExternoClient.obtenerResponsableSede(1L));
        // Assert
        mockServer.verify();
    }

    @Test
    @DisplayName("obtenerResponsableSede throws ServicioExternoException on network error")
    void obtenerResponsableSede_ShouldThrowExceptionOnNetworkError() {
        // Arrange
        prepareMockServer(false);

        mockServer.expect(requestTo("https://mallba3.lcc.uma.es/sedes/1"))
                .andRespond(request -> { throw new java.io.IOException("Network error"); });

        // Act & Assert
        assertThrows(ServicioExternoException.class, () -> 
                servicioExternoClient.obtenerResponsableSede(1L));
    }

    @Test
    @Tag("integration")
    @DisplayName("Integration: obtenerResponsableSede returns a valid idUsuario from the real service")
    void obtenerResponsableSede_IntegrationTest() {
        // Arrange
        Long idSede = 1L;
        
        // Act
        Long result = servicioExternoClient.obtenerResponsableSede(idSede);

        // Assert
        assertNotNull(result, "El ID del responsable no debería ser null si la sede 1 existe");
    }
}
