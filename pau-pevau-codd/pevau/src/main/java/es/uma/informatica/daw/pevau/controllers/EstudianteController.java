package es.uma.informatica.daw.pevau.controllers;

import es.uma.informatica.daw.pevau.dtos.Estudiante;
import es.uma.informatica.daw.pevau.dtos.EstudianteNuevo;
import es.uma.informatica.daw.pevau.dtos.ImportacionEstudiantes;
import es.uma.informatica.daw.pevau.excepciones.ConvocatoriaActualNoEncontradaException;
import es.uma.informatica.daw.pevau.excepciones.DniDuplicadoException;
import es.uma.informatica.daw.pevau.excepciones.ParticipacionNoEncontradaException;
import es.uma.informatica.daw.pevau.excepciones.ServicioExternoException;
import es.uma.informatica.daw.pevau.excepciones.ViolacionReglaNegocioException;
import es.uma.informatica.daw.pevau.excepciones.InstitutoNotFoundException;
import es.uma.informatica.daw.pevau.excepciones.EstudianteNotFoundException;
import es.uma.informatica.daw.pevau.servicios.ServicioExternoClient;
import es.uma.informatica.daw.pevau.servicios.ServicioEstudiante;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/estudiantes")
public class EstudianteController {

    private final ServicioEstudiante estudianteService;
    private final ServicioExternoClient servicioExternoClient;

    public EstudianteController(ServicioEstudiante estudianteService, ServicioExternoClient servicioExternoClient) {
        this.estudianteService = estudianteService;
        this.servicioExternoClient = servicioExternoClient;
    }

    /**
     * Devuelve la lista de estudiantes que tienen participación en una convocatoria,
     * opcionalmente filtrada por sede.
     */
    @GetMapping
    public ResponseEntity<List<Estudiante>> consultarEstudiantes(
            @RequestParam(required = false) Long idSede,
            @RequestParam(required = false) Long idConvocatoria,
            Authentication authentication) {

        boolean esAltoCargo = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("VICERRECTORADO") || a.getAuthority().equals("ADMINISTRADOR"));

        if (!esAltoCargo) {
            if (idSede == null) {
                throw new AccessDeniedException("Acceso no autorizado");
            }
            Long idResponsable = servicioExternoClient.obtenerResponsableSede(idSede);
            if (!idResponsable.toString().equals(authentication.getName())) {
                throw new AccessDeniedException("Acceso no autorizado");
            }
        }

        List<Estudiante> estudiantes = estudianteService
                .consultarEstudiantes(idSede, idConvocatoria);

        return ResponseEntity.ok(estudiantes);
    }

    /**
     * Crea un nuevo estudiante con participación en la convocatoria actual.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('VICERRECTORADO', 'ADMINISTRADOR')")
    public ResponseEntity<Estudiante> crearEstudiante(
            @RequestBody EstudianteNuevo estudianteNuevo) {

        Estudiante creado = estudianteService
                .crearEstudiante(estudianteNuevo);

        return ResponseEntity.status(201).body(creado);
    }

    /**
     * Devuelve información completa de un estudiante para una convocatoria concreta.
     */
    @GetMapping("/{idEstudiante}")
    public ResponseEntity<Estudiante> consultarEstudiante(
            @PathVariable Long idEstudiante,
            @RequestParam(required = false) Long idConvocatoria,
            Authentication authentication) {

        boolean esAltoCargo = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("VICERRECTORADO") || a.getAuthority().equals("ADMINISTRADOR"));

        Estudiante estudiante = estudianteService
                .consultarEstudiante(idEstudiante, idConvocatoria);

        if (!esAltoCargo) {
            Long idResponsable = servicioExternoClient.obtenerResponsableSede(estudiante.idSede());
            if (!idResponsable.toString().equals(authentication.getName())) {
                throw new AccessDeniedException("Acceso no autorizado");
            }
        }

        return ResponseEntity.ok(estudiante);
    }

    /**
     * Actualiza los datos personales del estudiante y su participación en la convocatoria actual.
     */
    @PutMapping("/{idEstudiante}")
    @PreAuthorize("hasAnyAuthority('VICERRECTORADO', 'ADMINISTRADOR')")
    public ResponseEntity<Estudiante> actualizarEstudiante(
            @PathVariable Long idEstudiante,
            @RequestBody EstudianteNuevo estudianteNuevo) {

        Estudiante actualizado = estudianteService
                .actualizarEstudiante(idEstudiante, estudianteNuevo);

        return ResponseEntity.ok(actualizado);
    }

    /**
     * Elimina la participación del estudiante en la convocatoria actual.
     */
    @DeleteMapping("/{idEstudiante}")
    @PreAuthorize("hasAnyAuthority('VICERRECTORADO', 'ADMINISTRADOR')")
    public ResponseEntity<Void> eliminarEstudiante(
            @PathVariable Long idEstudiante) {

        estudianteService.eliminarEstudiante(idEstudiante);

        return ResponseEntity.noContent().build();
    }

    /**
     * Importa estudiantes desde un fichero CSV para la convocatoria actual.
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyAuthority('VICERRECTORADO', 'ADMINISTRADOR')")
    public ResponseEntity<ImportacionEstudiantes> importarEstudiantes(
            @RequestParam("ficheroEstudiantes") MultipartFile ficheroEstudiantes) {

        ImportacionEstudiantes resultado = estudianteService
                .importarEstudiantes(ficheroEstudiantes);

        return ResponseEntity.ok(resultado);
    }

    /**
     * Maneja errores de formato y binding de Spring Web para evitar el enmascaramiento con 403.
     */
    @ExceptionHandler({
        ServletRequestBindingException.class,
        HttpMediaTypeNotSupportedException.class,
        MaxUploadSizeExceededException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Bad Request: " + e.getMessage()));
    }

    @ExceptionHandler(DniDuplicadoException.class)
    public ResponseEntity<String> handleDniDuplicado(DniDuplicadoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler({ParticipacionNoEncontradaException.class, EstudianteNotFoundException.class, InstitutoNotFoundException.class})
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(ViolacionReglaNegocioException.class)
    public ResponseEntity<String> handleReglaNegocio(ViolacionReglaNegocioException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler({ServicioExternoException.class, ConvocatoriaActualNoEncontradaException.class})
    public ResponseEntity<String> handleExternalErrors(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acceso no autorizado");
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, String>> handleInternalError(NullPointerException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error: A null value was encountered. This is often caused by missing request data or a service communication failure."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error: " + e.getMessage());
    }
}