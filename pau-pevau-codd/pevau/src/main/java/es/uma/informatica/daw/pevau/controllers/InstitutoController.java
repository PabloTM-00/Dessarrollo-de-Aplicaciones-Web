package es.uma.informatica.daw.pevau.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.uma.informatica.daw.pevau.dtos.Instituto;
import es.uma.informatica.daw.pevau.servicios.ServicioInstituto;
import es.uma.informatica.daw.pevau.excepciones.InstitutoNotFoundException;
import es.uma.informatica.daw.pevau.excepciones.InstitutoAsociadoException;

@RestController
@RequestMapping("/institutos")
@PreAuthorize("hasAnyAuthority('VICERRECTORADO', 'ADMINISTRADOR')")
public class InstitutoController {

    private final ServicioInstituto institutoService;

    public InstitutoController(ServicioInstituto institutoService) {
        this.institutoService = institutoService;
    }

    /**
     * Consulta todos los institutos.
     * @return ResponseEntity<List<Instituto>>
     */
    @GetMapping
    public ResponseEntity<List<Instituto>> consultarInstitutos() {
        return ResponseEntity.ok(institutoService.obtenerTodos());
    }
    
    /**
     * Crea un nuevo instituto.
     * @param instituto El DTO con los datos del instituto a crear.
     * @return ResponseEntity<Instituto>
     */
    @PostMapping
    public ResponseEntity<Instituto> crearInstituto(@RequestBody Instituto instituto) {
        Instituto creado = institutoService.crear(instituto);
        URI location = URI.create("/institutos/" + creado.id());
        return ResponseEntity.created(location).body(creado);
    }

    /**
     * Consulta un instituto por su ID.
     * @param idInstituto El ID del instituto a consultar.
     * @return ResponseEntity<Instituto>
     */
    @GetMapping("/{idInstituto}")
    public ResponseEntity<Instituto> consultarInstituto(@PathVariable Long idInstituto) {
        return ResponseEntity.ok(institutoService.obtenerPorId(idInstituto));
    }


    /**
     * Actualiza un instituto existente.
     * @param idInstituto El ID del instituto a actualizar.
     * @param instituto El DTO con los nuevos datos del instituto.
     * @return ResponseEntity<Instituto>
     */
    @PutMapping("/{idInstituto}")
    public ResponseEntity<Instituto> actualizarInstituto(
            @PathVariable Long idInstituto,
            @RequestBody Instituto instituto) {
        return ResponseEntity.ok(institutoService.actualizar(idInstituto, instituto));
    }

    /**
     * Elimina un instituto existente.
     * @param idInstituto El ID del instituto a eliminar.
     * @return ResponseEntity<Void>
     */
    @DeleteMapping("/{idInstituto}")
    public ResponseEntity<Void> eliminarInstituto(@PathVariable Long idInstituto) {
        institutoService.eliminar(idInstituto);
        return ResponseEntity.ok().build();
    }

    /**
     * Maneja la excepción cuando un instituto no es encontrado.
     * @param e La excepción lanzada.
     * @return ResponseEntity<Void> con status 404.
     */
    @ExceptionHandler(InstitutoNotFoundException.class)
    public ResponseEntity<Void> handleNotFound(InstitutoNotFoundException e) {
        return ResponseEntity.notFound().build();
    }

    /**
     * Maneja la excepción cuando se intenta eliminar un instituto con estudiantes asociados.
     * @param e La excepción lanzada.
     * @return ResponseEntity<String> con status 409 y el mensaje de error.
     */
    @ExceptionHandler(InstitutoAsociadoException.class)
    public ResponseEntity<String> handleConflict(InstitutoAsociadoException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }

}