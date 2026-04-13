package es.uma.daw.libros.servicios;

import es.uma.daw.libros.dtos.LibroDTO;
import es.uma.daw.libros.excepciones.LibroNoEncontrado;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibroService {

    private List<LibroDTO> libros = new ArrayList<>();
    private long contadorId = 1;

    public List<LibroDTO> obtenerTodos() {
        return libros;
    }

    public LibroDTO aniadirLibro(LibroDTO libro) {
        libro.setId(contadorId++);
        libros.add(libro);
        return libro;
    }

    public LibroDTO obtenerPorId(Long id) {
        return libros.stream()
                .filter(l -> l.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new LibroNoEncontrado("Libro no encontrado"));
    }

    public LibroDTO actualizarLibro(Long id, LibroDTO libroActualizado) {
        LibroDTO libroExistente = obtenerPorId(id);
        libroExistente.setTitulo(libroActualizado.getTitulo());
        libroExistente.setAutor(libroActualizado.getAutor());
        libroExistente.setAnio(libroActualizado.getAnio());
        return libroExistente;
    }

    public void eliminarLibro(Long id) {
        LibroDTO libro = obtenerPorId(id);
        libros.remove(libro);
    }

    public List<LibroDTO> buscarPorAutor(String autor) {
        return libros.stream()
                .filter(l -> l.getAutor().equalsIgnoreCase(autor))
                .collect(Collectors.toList());
    }
}
