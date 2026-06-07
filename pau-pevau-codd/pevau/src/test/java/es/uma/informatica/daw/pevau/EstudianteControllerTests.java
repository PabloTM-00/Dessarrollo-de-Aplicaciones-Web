package es.uma.informatica.daw.pevau;

import es.uma.informatica.daw.pevau.controllers.EstudianteController;
import es.uma.informatica.daw.pevau.dtos.*;
import es.uma.informatica.daw.pevau.excepciones.*;
import es.uma.informatica.daw.pevau.servicios.ServicioEstudiante;
import es.uma.informatica.daw.pevau.servicios.ServicioExternoClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EstudianteControllerTests {

    private ServicioEstudiante servicioEstudiante;
    private ServicioExternoClient servicioExternoClient;

    private EstudianteController controller;

    @BeforeEach
    void setUp() {

        servicioEstudiante = mock(ServicioEstudiante.class);
        servicioExternoClient = mock(ServicioExternoClient.class);

        controller = new EstudianteController(
                servicioEstudiante,
                servicioExternoClient
        );
    }

    // =========================================================
    // MÉTODOS AUXILIARES
    // =========================================================

    private Estudiante crearEstudiante() {

        return new Estudiante(
                1L,
                new NombreCompleto("Juan", "Perez", "Lopez"),
                "12345678A",
                "666666666",
                "juan@test.com",
                Set.of(new Materia(10L, "Matematicas", false)),
                2L,
                new Instituto(
                        1L,
                        "IES Test",
                        "Dir1",
                        "Dir2",
                        "Malaga",
                        29001,
                        "España"
                ),
                false
        );
    }

    private EstudianteNuevo crearDTO() {

        return new EstudianteNuevo(
                new NombreCompleto("Juan", "Perez", "Lopez"),
                "12345678A",
                "666666666",
                "juan@test.com",
                Set.of(10L),
                1L,
                2L,
                false
        );
    }

    private Authentication adminAuth() {

        return new TestingAuthenticationToken(
                "1",
                null,
                "ADMINISTRADOR"
        );
    }

    private Authentication vicerrectoradoAuth() {

        return new TestingAuthenticationToken(
                "1",
                null,
                "VICERRECTORADO"
        );
    }

    private Authentication responsableAuth() {

        return new TestingAuthenticationToken(
                "5",
                null,
                "RESPONSABLE_SEDE"
        );
    }

    // =========================================================
    // consultarEstudiantes
    // =========================================================

    @Test
    @DisplayName("Debe devolver estudiantes siendo administrador")
    void consultarEstudiantes_admin() {

        // Arrange
        when(servicioEstudiante.consultarEstudiantes(null, null))
                .thenReturn(List.of(crearEstudiante()));

        // Act
        ResponseEntity<List<Estudiante>> response =
                controller.consultarEstudiantes(
                        null,
                        null,
                        adminAuth()
                );

        // Assert
        assertAll(
                () -> assertEquals(200, response.getStatusCode().value()),
                () -> assertEquals(1, response.getBody().size())
        );
    }

    @Test
    @DisplayName("Debe permitir acceso a vicerrectorado")
    void consultarEstudiantes_vicerrectorado() {

        // Arrange
        when(servicioEstudiante.consultarEstudiantes(null, null))
                .thenReturn(List.of(crearEstudiante()));

        // Act
        ResponseEntity<List<Estudiante>> response =
                controller.consultarEstudiantes(
                        null,
                        null,
                        vicerrectoradoAuth()
                );

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe lanzar AccessDeniedException si responsable no indica sede")
    void consultarEstudiantes_responsableSinSede() {

        // Arrange + Act + Assert
        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> controller.consultarEstudiantes(
                        null,
                        null,
                        responsableAuth()
                )
        );
    }

    @Test
    @DisplayName("Debe devolver estudiantes cuando el responsable coincide")
    void consultarEstudiantes_responsableOk() {

        // Arrange
        when(servicioExternoClient.obtenerResponsableSede(2L))
                .thenReturn(5L);

        when(servicioEstudiante.consultarEstudiantes(2L, null))
                .thenReturn(List.of(crearEstudiante()));

        // Act
        ResponseEntity<List<Estudiante>> response =
                controller.consultarEstudiantes(
                        2L,
                        null,
                        responsableAuth()
                );

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe lanzar AccessDeniedException cuando el responsable no coincide")
    void consultarEstudiantes_responsableIncorrecto() {

        // Arrange
        when(servicioExternoClient.obtenerResponsableSede(2L))
                .thenReturn(99L);

        // Act + Assert
        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> controller.consultarEstudiantes(
                        2L,
                        null,
                        responsableAuth()
                )
        );
    }

    // =========================================================
    // crearEstudiante
    // =========================================================

    @Test
    @DisplayName("Debe crear estudiante correctamente")
    void crearEstudiante_ok() {

        // Arrange
        when(servicioEstudiante.crearEstudiante(any()))
                .thenReturn(crearEstudiante());

        // Act
        ResponseEntity<Estudiante> response =
                controller.crearEstudiante(crearDTO());

        // Assert
        assertAll(
                () -> assertEquals(201, response.getStatusCode().value()),
                () -> assertNotNull(response.getBody()),
                () -> assertEquals(
                        "12345678A",
                        response.getBody().dni()
                )
        );
    }

    // =========================================================
    // consultarEstudiante
    // =========================================================

    @Test
    @DisplayName("Debe consultar estudiante siendo administrador")
    void consultarEstudiante_admin() {

        // Arrange
        when(servicioEstudiante.consultarEstudiante(1L, null))
                .thenReturn(crearEstudiante());

        // Act
        ResponseEntity<Estudiante> response =
                controller.consultarEstudiante(
                        1L,
                        null,
                        adminAuth()
                );

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe permitir acceso a vicerrectorado para consultar un estudiante")
    void consultarEstudiante_vicerrectorado() {

        // Arrange
        when(servicioEstudiante.consultarEstudiante(1L, null))
                .thenReturn(crearEstudiante());

        // Act
        ResponseEntity<Estudiante> response =
                controller.consultarEstudiante(
                        1L,
                        null,
                        vicerrectoradoAuth()
                );

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe consultar estudiante cuando el responsable coincide")
    void consultarEstudiante_responsableOk() {

        // Arrange
        when(servicioEstudiante.consultarEstudiante(1L, null))
                .thenReturn(crearEstudiante());

        when(servicioExternoClient.obtenerResponsableSede(2L))
                .thenReturn(5L);

        // Act
        ResponseEntity<Estudiante> response =
                controller.consultarEstudiante(
                        1L,
                        null,
                        responsableAuth()
                );

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe permitir acceso cuando responsable coincide exactamente")
    void consultarEstudiante_responsableExacto() {

        // Arrange
        when(servicioEstudiante.consultarEstudiante(1L, null))
                .thenReturn(crearEstudiante());

        when(servicioExternoClient.obtenerResponsableSede(2L))
                .thenReturn(5L);

        Authentication auth =
                new TestingAuthenticationToken(
                        "5",
                        null,
                        "RESPONSABLE_SEDE"
                );

        // Act
        ResponseEntity<Estudiante> response =
                controller.consultarEstudiante(
                        1L,
                        null,
                        auth
                );

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe lanzar AccessDeniedException cuando el responsable no coincide")
    void consultarEstudiante_forbidden() {

        // Arrange
        when(servicioEstudiante.consultarEstudiante(1L, null))
                .thenReturn(crearEstudiante());

        when(servicioExternoClient.obtenerResponsableSede(2L))
                .thenReturn(99L);

        // Act + Assert
        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> controller.consultarEstudiante(
                        1L,
                        null,
                        responsableAuth()
                )
        );
    }

    // =========================================================
    // actualizarEstudiante
    // =========================================================

    @Test
    @DisplayName("Debe actualizar estudiante correctamente")
    void actualizarEstudiante_ok() {

        // Arrange
        when(servicioEstudiante.actualizarEstudiante(eq(1L), any()))
                .thenReturn(crearEstudiante());

        // Act
        ResponseEntity<Estudiante> response =
                controller.actualizarEstudiante(
                        1L,
                        crearDTO()
                );

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    // =========================================================
    // eliminarEstudiante
    // =========================================================

    @Test
    @DisplayName("Debe eliminar estudiante correctamente")
    void eliminarEstudiante_ok() {

        // Arrange
        doNothing().when(servicioEstudiante)
                .eliminarEstudiante(1L);

        // Act
        ResponseEntity<Void> response =
                controller.eliminarEstudiante(1L);

        // Assert
        assertEquals(204, response.getStatusCode().value());

        verify(servicioEstudiante)
                .eliminarEstudiante(1L);
    }

    // =========================================================
    // importarEstudiantes
    // =========================================================

    @Test
    @DisplayName("Debe importar estudiantes correctamente")
    void importarEstudiantes_ok() {

        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "ficheroEstudiantes",
                "test.csv",
                "text/csv",
                "csv".getBytes()
        );

        when(servicioEstudiante.importarEstudiantes(any()))
                .thenReturn(
                        new ImportacionEstudiantes(
                                List.of(crearEstudiante()),
                                List.of()
                        )
                );

        // Act
        ResponseEntity<ImportacionEstudiantes> response =
                controller.importarEstudiantes(file);

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    // =========================================================
    // EXCEPTION HANDLERS
    // =========================================================

    @Test
    @DisplayName("Debe manejar DniDuplicadoException")
    void handleDniDuplicado() {

        // Arrange
        DniDuplicadoException ex =
                new DniDuplicadoException("Duplicado");

        // Act
        ResponseEntity<String> response =
                controller.handleDniDuplicado(ex);

        // Assert
        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe manejar not found")
    void handleNotFound() {

        // Act
        ResponseEntity<Void> response =
                controller.handleNotFound();

        // Assert
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe manejar InstitutoNotFoundException")
    void handleNotFoundInstituto() {

        // Act
        ResponseEntity<Void> response =
                controller.handleNotFound();

        // Assert
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe manejar reglas de negocio")
    void handleReglaNegocio() {

        // Arrange
        ViolacionReglaNegocioException ex =
                new ViolacionReglaNegocioException("Error");

        // Act
        ResponseEntity<String> response =
                controller.handleReglaNegocio(ex);

        // Assert
        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe manejar errores externos")
    void handleExternalErrors() {

        // Arrange
        ServicioExternoException ex =
                new ServicioExternoException("Error");

        // Act
        ResponseEntity<String> response =
                controller.handleExternalErrors(ex);

        // Assert
        assertEquals(503, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe manejar ConvocatoriaActualNoEncontradaException")
    void handleConvocatoriaActualNoEncontrada() {

        // Arrange
        ConvocatoriaActualNoEncontradaException ex =
                new ConvocatoriaActualNoEncontradaException();

        // Act
        ResponseEntity<String> response =
                controller.handleExternalErrors(ex);

        // Assert
        assertEquals(503, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe manejar access denied")
    void handleAccessDenied() {

        // Arrange
        org.springframework.security.access.AccessDeniedException ex =
                new org.springframework.security.access.AccessDeniedException("Forbidden");

        // Act
        ResponseEntity<String> response =
                controller.handleAccessDenied(ex);

        // Assert
        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe manejar NullPointerException")
    void handleNullPointer() {

        // Arrange
        NullPointerException ex =
                new NullPointerException();

        // Act
        ResponseEntity<Map<String, String>> response =
                controller.handleInternalError(ex);

        // Assert
        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe manejar excepciones genéricas")
    void handleGenericException() {

        // Arrange
        Exception ex = new Exception("Boom");

        // Act
        ResponseEntity<String> response =
                controller.handleGenericException(ex);

        // Assert
        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Debe manejar bad request")
    void handleBadRequest() {

        // Arrange
        IllegalArgumentException ex =
                new IllegalArgumentException("Bad");

        // Act
        ResponseEntity<Map<String, String>> response =
                controller.handleBadRequest(ex);

        // Assert
        assertEquals(400, response.getStatusCode().value());
    }
}