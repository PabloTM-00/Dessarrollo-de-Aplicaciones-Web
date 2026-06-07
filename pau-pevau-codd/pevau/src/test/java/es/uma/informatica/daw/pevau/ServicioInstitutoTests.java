package es.uma.informatica.daw.pevau;

import es.uma.informatica.daw.pevau.dtos.Instituto;
import es.uma.informatica.daw.pevau.entidades.EstudianteEntity;
import es.uma.informatica.daw.pevau.entidades.InstitutoEntity;
import es.uma.informatica.daw.pevau.excepciones.InstitutoAsociadoException;
import es.uma.informatica.daw.pevau.excepciones.InstitutoNotFoundException;
import es.uma.informatica.daw.pevau.repositorios.RepositorioEstudiante;
import es.uma.informatica.daw.pevau.repositorios.RepositorioInstituto;
import es.uma.informatica.daw.pevau.servicios.ServicioInstituto;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ServicioInstitutoTests {

    @Autowired
    private ServicioInstituto servicioInstituto;

    @Autowired
    private RepositorioInstituto repositorioInstituto;

    @Autowired
    private RepositorioEstudiante repositorioEstudiante;

    @BeforeEach
    void setUp() {
        // Limpiamos los repositorios antes de cada prueba para asegurar la independencia.
        // Es importante borrar primero los estudiantes porque tienen una clave foránea 
        // que apunta a los institutos.
        repositorioEstudiante.deleteAll();
        repositorioInstituto.deleteAll();
    }

    //-------------------------------obtenerTodos-------------------------------//
    @Test
    @DisplayName("[devuelveTodos] devuelve el número de institutos correctos")
    void obtenerTodosInsitutos() {

        InstitutoEntity i1 = new InstitutoEntity();
        i1.setNombre("IES 1");

        InstitutoEntity i2 = new InstitutoEntity();
        i2.setNombre("IES 2");

        repositorioInstituto.save(i1);
        repositorioInstituto.save(i2);

        List<Instituto> lista = servicioInstituto.obtenerTodos();

        assertThat(lista).hasSize(2);
    }

    @Test
    @DisplayName("[obtenerTodos] devuelve 0 cuando la lista está vacía")
    void obtenerNigunInstituto() {
        List<Instituto> lista = servicioInstituto.obtenerTodos();

        assertThat(lista).hasSize(0);
    }

    //-------------------------------obtenerPorId-------------------------------//
    @Test
    @DisplayName("[obtenerPorId] obtiene un instituto existente")
    void obtenerInstitutoExistente() {

        InstitutoEntity entity = new InstitutoEntity();
        entity.setNombre("IES Test");

        entity = repositorioInstituto.save(entity);

        Instituto dto = servicioInstituto.obtenerPorId(entity.getId());

        assertThat(dto).isNotNull();
        assertThat(dto.nombre()).isEqualTo("IES Test");
    }

    @Test
    @DisplayName("[obtenerPorId] lanza excepción al buscar un instituto inexistente")
    void obtenerInstitutoInexistente() {

        assertThatThrownBy(() ->
                servicioInstituto.obtenerPorId(999L))
                .isInstanceOf(InstitutoNotFoundException.class);
    }

    //-------------------------------crear-------------------------------//

    @Test
    @DisplayName("[crear] crea un instituto correctamente")
    void crearInstituto(){
        Instituto nuevo = new Instituto(
                null,
                "IES Málaga",
                "Calle A",
                "Bloque B",
                "Málaga",
                29001,
                "España"
        );

        Instituto creado = servicioInstituto.crear(nuevo);

        assertNotNull(creado);
        assertNotNull(creado.id());
        assertEquals("IES Málaga", creado.nombre());
        assertEquals("Málaga", creado.localidad());
    }

    //-------------------------------actualizar-------------------------------//
    @Test
    @DisplayName("[ctualizar] actualiza un instituto existente")
    void actualizarInstituto() {

        InstitutoEntity entity = new InstitutoEntity();
        entity.setNombre("Viejo");

        entity = repositorioInstituto.save(entity);

        Instituto nuevosDatos = new Instituto(
                null,
                "Nuevo",
                "Dir1",
                "Dir2",
                "Sevilla",
                41001,
                "España"
        );

        Instituto actualizado =
                servicioInstituto.actualizar(entity.getId(), nuevosDatos);

        assertThat(actualizado.nombre()).isEqualTo("Nuevo");
        assertThat(actualizado.localidad()).isEqualTo("Sevilla");
    }

    @Test
    @DisplayName("[actualizar] lanza excepción al actualizar un instituto inexistente")
    void actualizarInstitutoInexistente() {

        Instituto dto = new Instituto(
                null,
                "Nuevo",
                "Dir1",
                "Dir2",
                "Sevilla",
                41001,
                "España"
        );

        assertThatThrownBy(() ->
                servicioInstituto.actualizar(999L, dto))
                .isInstanceOf(InstitutoNotFoundException.class);
    }

    //-------------------------------eliminar-------------------------------//
    @Test
    @DisplayName("[eliminar] elimina un instituto sin estudiantes")
    void eliminarInstituto() {

        InstitutoEntity entity = new InstitutoEntity();
        entity.setNombre("IES");

        entity = repositorioInstituto.save(entity);

        servicioInstituto.eliminar(entity.getId());

        assertThat(repositorioInstituto.findById(entity.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("[eliminar] lanza excepción al eliminar un instituto inexistente")
    void eliminarInstitutoInexistente() {

        assertThatThrownBy(() ->
                servicioInstituto.eliminar(999L))
                .isInstanceOf(InstitutoNotFoundException.class);
    }

    @Test
    @Transactional
    @DisplayName("[eliminar] lanza excepción al eliminar un instituto con estudiantes")
    void eliminarInstitutoConEstudiantes() {

        InstitutoEntity instituto = new InstitutoEntity();
        instituto.setNombre("IES");

        instituto.setEstudiantes(new HashSet<>());
        instituto = repositorioInstituto.save(instituto);

        EstudianteEntity estudiante = new EstudianteEntity();
        estudiante.setApellido1("García");
        estudiante.setApellido2("López");
        estudiante.setNombre("Juan");
        estudiante.setDni("37295721h");
        estudiante.setTelefono("2340232");
        estudiante.setEmail("juanjo@uma.es");
        estudiante.setIdConvocatoria(23L);
        estudiante.setIdSede(234L);
        estudiante.setNoEliminar(false);

        estudiante.setInstituto(instituto);
        instituto.getEstudiantes().add(estudiante);

        repositorioEstudiante.save(estudiante); // Guarda el estudiante directamente

        final Long id = instituto.getId();

        assertThatThrownBy(() -> servicioInstituto.eliminar(id))
                .isInstanceOf(InstitutoAsociadoException.class);
    }

    @Test
    @DisplayName("[eliminar] permite eliminar cuando la lista de estudiantes es null")
    void eliminarInstitutoConEstudiantesNull() {

        InstitutoEntity entity = new InstitutoEntity();
        entity.setNombre("IES Null");
        entity.setEstudiantes(null);

        entity = repositorioInstituto.save(entity);

        servicioInstituto.eliminar(entity.getId());

        assertThat(repositorioInstituto.findById(entity.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("[eliminar] permite eliminar cuando la lista de estudiantes está vacía")
    void eliminarInstitutoConEstudiantesVacia() {

        InstitutoEntity entity = new InstitutoEntity();
        entity.setNombre("IES Vacia");
        entity.setEstudiantes(new HashSet<>());

        entity = repositorioInstituto.save(entity);

        servicioInstituto.eliminar(entity.getId());

        assertThat(repositorioInstituto.findById(entity.getId()))
                .isEmpty();
    }
}