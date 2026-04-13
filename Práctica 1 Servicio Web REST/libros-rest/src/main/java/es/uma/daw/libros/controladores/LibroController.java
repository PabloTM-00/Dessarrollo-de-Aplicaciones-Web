package es.uma.daw.libros.controladores;

import es.uma.daw.libros.dtos.LibroDTO;
import es.uma.daw.libros.servicios.LibroService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/libros")
public class LibroController {

    private final LibroService servicio;

    public LibroController(LibroService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("")
    public List<LibroDTO> obtenerTodos() {
        return servicio.obtenerTodos();
    }

    @PostMapping("")
    public ResponseEntity<LibroDTO> aniadirLibro(@RequestBody LibroDTO libro, UriComponentsBuilder uriBuilder) {
        LibroDTO aniadido = servicio.aniadirLibro(libro);
        URI location = uriBuilder.path("/libros/{id}").buildAndExpand(aniadido.getId()).toUri();
        return ResponseEntity.created(location).body(aniadido);
    }

    @GetMapping("/{id}")
    public LibroDTO obtenerPorId(@PathVariable Long id) {
        return servicio.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public LibroDTO actualizarLibro(@PathVariable Long id, @RequestBody LibroDTO libro) {
        return servicio.actualizarLibro(id, libro);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarLibro(@PathVariable Long id) {
        servicio.eliminarLibro(id);
    }

    @GetMapping("/autor/{autor}")
    public List<LibroDTO> buscarPorAutor(@PathVariable String autor) {
        return servicio.buscarPorAutor(autor);
    }
}
