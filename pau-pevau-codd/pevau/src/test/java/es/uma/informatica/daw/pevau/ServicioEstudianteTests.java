package es.uma.informatica.daw.pevau;

import es.uma.informatica.daw.pevau.dtos.*;
import es.uma.informatica.daw.pevau.entidades.EstudianteEntity;
import es.uma.informatica.daw.pevau.entidades.InstitutoEntity;
import es.uma.informatica.daw.pevau.excepciones.*;
import es.uma.informatica.daw.pevau.repositorios.RepositorioEstudiante;
import es.uma.informatica.daw.pevau.repositorios.RepositorioInstituto;
import es.uma.informatica.daw.pevau.servicios.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioEstudianteTests {

    @Mock
    private RepositorioEstudiante repositorioEstudiante;

    @Mock
    private RepositorioInstituto repositorioInstituto;

    @Mock
    private ServicioExternoClient servicioExternoClient;

    @InjectMocks
    private ServicioEstudiante servicioEstudiante;

    private EstudianteEntity estudianteEntity;
    private InstitutoEntity institutoEntity;

    @BeforeEach
    void setUp() {

        // Arrange
        institutoEntity = new InstitutoEntity();
        institutoEntity.setId(1L);
        institutoEntity.setNombre("IES Test");
        institutoEntity.setDireccion1("Direccion 1");
        institutoEntity.setDireccion2("Direccion 2");
        institutoEntity.setLocalidad("Malaga");
        institutoEntity.setCodigoPostal(29001);
        institutoEntity.setPais("España");

        estudianteEntity = new EstudianteEntity();
        estudianteEntity.setId(1L);
        estudianteEntity.setNombre("Juan");
        estudianteEntity.setApellido1("Perez");
        estudianteEntity.setApellido2("Lopez");
        estudianteEntity.setDni("12345678A");
        estudianteEntity.setTelefono("666666666");
        estudianteEntity.setEmail("juan@test.com");
        estudianteEntity.setInstituto(institutoEntity);
        estudianteEntity.setIdConvocatoria(1L);
        estudianteEntity.setIdSede(2L);
        estudianteEntity.setMateriasMatriculadas(Set.of(10L));
        estudianteEntity.setNoEliminar(false);
    }

    // =========================================================
    // consultarEstudiante
    // =========================================================

    @Test
    @DisplayName("Debe consultar correctamente un estudiante existente")
    void consultarEstudiante_ok() {

        // Arrange
        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.of(estudianteEntity));

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of(
                        new Materia(10L, "Matematicas", false)
                ));

        // Act
        Estudiante resultado =
                servicioEstudiante.consultarEstudiante(1L, 1L);

        // Assert
        assertAll(
                () -> assertNotNull(resultado),
                () -> assertEquals("12345678A", resultado.dni()),
                () -> assertEquals(2L, resultado.idSede()),
                () -> assertFalse(resultado.noEliminar()),
                () -> assertEquals("Juan", resultado.nombreCompleto().nombre()),
                () -> assertEquals("Perez", resultado.nombreCompleto().apellido1()),
                () -> assertEquals("Lopez", resultado.nombreCompleto().apellido2())
        );

        verify(repositorioEstudiante).findById(1L);
        verify(servicioExternoClient).obtenerMateriasBatch(any());
    }

    @Test
    @DisplayName("Debe lanzar EstudianteNotFoundException cuando el estudiante no existe")
    void consultarEstudiante_notFound() {

        // Arrange
        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                EstudianteNotFoundException.class,
                () -> servicioEstudiante.consultarEstudiante(1L, 1L)
        );

        verify(repositorioEstudiante).findById(1L);
    }

    @Test
    @DisplayName("Debe lanzar ParticipacionNoEncontradaException cuando la convocatoria no coincide")
    void consultarEstudiante_participacionNoEncontrada() {

        // Arrange
        estudianteEntity.setIdConvocatoria(99L);

        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.of(estudianteEntity));

        // Act + Assert
        assertThrows(
                ParticipacionNoEncontradaException.class,
                () -> servicioEstudiante.consultarEstudiante(1L, 1L)
        );
    }

    @Test
    @DisplayName("Debe usar la convocatoria actual cuando idConvocatoria es null")
    void consultarEstudiante_convocatoriaActual() {

        // Arrange
        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.of(estudianteEntity));

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of());

        // Act
        servicioEstudiante.consultarEstudiante(1L, null);

        // Assert
        verify(servicioExternoClient).obtenerConvocatoriaActual();
    }

    // =========================================================
    // consultarEstudiantes
    // =========================================================

    @Test
    @DisplayName("Debe consultar estudiantes sin filtrar por sede")
    void consultarEstudiantes_sinSede() {

        // Arrange
        when(repositorioEstudiante.findByIdConvocatoria(1L))
                .thenReturn(List.of(estudianteEntity));

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of());

        // Act
        List<Estudiante> resultado =
                servicioEstudiante.consultarEstudiantes(null, 1L);

        // Assert
        assertEquals(1, resultado.size());

        verify(repositorioEstudiante).findByIdConvocatoria(1L);
    }

    @Test
    @DisplayName("Debe consultar estudiantes filtrando por sede")
    void consultarEstudiantes_conSede() {

        // Arrange
        when(repositorioEstudiante.findByIdConvocatoriaAndIdSede(1L, 2L))
                .thenReturn(List.of(estudianteEntity));

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of());

        // Act
        List<Estudiante> resultado =
                servicioEstudiante.consultarEstudiantes(2L, 1L);

        // Assert
        assertEquals(1, resultado.size());

        verify(repositorioEstudiante)
                .findByIdConvocatoriaAndIdSede(1L, 2L);
    }

    @Test
    @DisplayName("Debe usar la convocatoria actual al consultar estudiantes")
    void consultarEstudiantes_convocatoriaActual() {

        // Arrange
        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(repositorioEstudiante.findByIdConvocatoria(1L))
                .thenReturn(List.of(estudianteEntity));

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of());

        // Act
        servicioEstudiante.consultarEstudiantes(null, null);

        // Assert
        verify(servicioExternoClient).obtenerConvocatoriaActual();
    }

    // =========================================================
    // crearEstudiante
    // =========================================================

    @Test
    @DisplayName("Debe crear correctamente un estudiante válido")
    void crearEstudiante_ok() {

        // Arrange
        EstudianteNuevo dto = crearDTO();

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(repositorioEstudiante.findByDniAndIdConvocatoria(any(), any()))
                .thenReturn(Optional.empty());

        when(repositorioInstituto.existsById(1L))
                .thenReturn(true);

        when(repositorioInstituto.getReferenceById(1L))
                .thenReturn(institutoEntity);

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of(
                        new Materia(10L, "Matematicas", false)
                ));

        // Act
        Estudiante resultado =
                servicioEstudiante.crearEstudiante(dto);

        // Assert
        assertAll(
                () -> assertEquals("12345678A", resultado.dni()),
                () -> assertEquals(2L, resultado.idSede()),
                () -> assertFalse(resultado.noEliminar())
        );

        verify(repositorioEstudiante).save(any());
    }

    @Test
    @DisplayName("Debe lanzar DniDuplicadoException cuando el DNI ya existe")
    void crearEstudiante_dniDuplicado() {

        // Arrange
        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(repositorioEstudiante.findByDniAndIdConvocatoria(any(), any()))
                .thenReturn(Optional.of(estudianteEntity));

        // Act + Assert
        assertThrows(
                DniDuplicadoException.class,
                () -> servicioEstudiante.crearEstudiante(crearDTO())
        );
    }

    @Test
    @DisplayName("Debe lanzar InstitutoNotFoundException cuando el instituto no existe")
    void crearEstudiante_institutoNoExiste() {

        // Arrange
        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(repositorioEstudiante.findByDniAndIdConvocatoria(any(), any()))
                .thenReturn(Optional.empty());

        when(repositorioInstituto.existsById(1L))
                .thenReturn(false);

        // Act + Assert
        assertThrows(
                InstitutoNotFoundException.class,
                () -> servicioEstudiante.crearEstudiante(crearDTO())
        );
    }

    @Test
    @DisplayName("Debe lanzar ViolacionReglaNegocioException cuando una materia está eliminada")
    void crearEstudiante_materiaEliminada() {

        // Arrange
        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(repositorioEstudiante.findByDniAndIdConvocatoria(any(), any()))
                .thenReturn(Optional.empty());

        when(repositorioInstituto.existsById(1L))
                .thenReturn(true);

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of(
                        new Materia(10L, "Matematicas", true)
                ));

        // Act + Assert
        assertThrows(
                ViolacionReglaNegocioException.class,
                () -> servicioEstudiante.crearEstudiante(crearDTO())
        );
    }

    // =========================================================
    // actualizarEstudiante
    // =========================================================

    @Test
    @DisplayName("Debe actualizar correctamente un estudiante")
    void actualizarEstudiante_ok() {

        // Arrange
        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.of(estudianteEntity));

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(repositorioInstituto.existsById(1L))
                .thenReturn(true);

        when(repositorioInstituto.getReferenceById(1L))
                .thenReturn(institutoEntity);

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of(
                        new Materia(10L, "Matematicas", false)
                ));

        when(repositorioEstudiante.save(any()))
                .thenReturn(estudianteEntity);

        // Act
        Estudiante resultado =
                servicioEstudiante.actualizarEstudiante(1L, crearDTO());

        // Assert
        assertEquals("12345678A", resultado.dni());

        verify(repositorioEstudiante).save(any());
    }

    @Test
    @DisplayName("Debe lanzar ViolacionReglaNegocioException cuando la convocatoria no es la actual")
    void actualizarEstudiante_convocatoriaIncorrecta() {

        // Arrange
        estudianteEntity.setIdConvocatoria(99L);

        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.of(estudianteEntity));

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        // Act + Assert
        assertThrows(
                ViolacionReglaNegocioException.class,
                () -> servicioEstudiante.actualizarEstudiante(1L, crearDTO())
        );
    }

    @Test
    @DisplayName("Debe lanzar ViolacionReglaNegocioException cuando se intenta revertir noEliminar")
    void actualizarEstudiante_noEliminarNoReversible() {

        // Arrange
        estudianteEntity.setNoEliminar(true);

        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.of(estudianteEntity));

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(repositorioInstituto.existsById(1L))
                .thenReturn(true);

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of());

        // Act + Assert
        assertThrows(
                ViolacionReglaNegocioException.class,
                () -> servicioEstudiante.actualizarEstudiante(1L, crearDTO())
        );
    }

    @Test
    @DisplayName("Debe permitir actualizar manteniendo noEliminar a true")
    void actualizarEstudiante_mantieneNoEliminarTrue() {

        // Arrange
        estudianteEntity.setNoEliminar(true);
        EstudianteNuevo dto = new EstudianteNuevo(
                new NombreCompleto("Juan", "Perez", "Lopez"),
                "12345678A",
                "666666666",
                "juan@test.com",
                Set.of(10L),
                1L,
                2L,
                true
        );

        when(repositorioEstudiante.findById(1L)).thenReturn(Optional.of(estudianteEntity));
        when(servicioExternoClient.obtenerConvocatoriaActual()).thenReturn(1L);
        when(repositorioInstituto.existsById(1L)).thenReturn(true);
        when(repositorioInstituto.getReferenceById(1L)).thenReturn(institutoEntity);
        when(servicioExternoClient.obtenerMateriasBatch(any())).thenReturn(List.of(new Materia(10L, "Matematicas", false)));
        when(repositorioEstudiante.save(any())).thenReturn(estudianteEntity);

        // Act
        Estudiante resultado = servicioEstudiante.actualizarEstudiante(1L, dto);

        // Assert
        assertTrue(resultado.noEliminar());
    }

    @Test
    @DisplayName("Debe comprobar duplicidad de DNI cuando el DNI cambia")
    void actualizarEstudiante_dniCambiado() {

        // Arrange
        estudianteEntity.setDni("OLD");

        EstudianteNuevo dto = new EstudianteNuevo(
                new NombreCompleto("Juan", "Perez", "Lopez"),
                "NEW",
                "666666666",
                "test@test.com",
                Set.of(10L),
                1L,
                2L,
                false
        );

        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.of(estudianteEntity));

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(repositorioEstudiante.findByDniAndIdConvocatoria("NEW", 1L))
                .thenReturn(Optional.empty());

        when(repositorioInstituto.existsById(1L))
                .thenReturn(true);

        when(repositorioInstituto.getReferenceById(1L))
                .thenReturn(institutoEntity);

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of(
                        new Materia(10L, "Matematicas", false)
                ));

        when(repositorioEstudiante.save(any()))
                .thenReturn(estudianteEntity);

        // Act
        servicioEstudiante.actualizarEstudiante(1L, dto);

        // Assert
        verify(repositorioEstudiante)
                .findByDniAndIdConvocatoria("NEW", 1L);
    }

    // =========================================================
    // eliminarEstudiante
    // =========================================================

    @Test
    @DisplayName("Debe eliminar correctamente un estudiante")
    void eliminarEstudiante_ok() {

        // Arrange
        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.of(estudianteEntity));

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        // Act
        servicioEstudiante.eliminarEstudiante(1L);

        // Assert
        verify(repositorioEstudiante).delete(estudianteEntity);
    }

    @Test
    @DisplayName("Debe lanzar ViolacionReglaNegocioException cuando noEliminar es true")
    void eliminarEstudiante_noEliminar() {

        // Arrange
        estudianteEntity.setNoEliminar(true);

        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.of(estudianteEntity));

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        // Act + Assert
        assertThrows(
                ViolacionReglaNegocioException.class,
                () -> servicioEstudiante.eliminarEstudiante(1L)
        );
    }

    @Test
    @DisplayName("Debe lanzar ParticipacionNoEncontradaException cuando la convocatoria no coincide")
    void eliminarEstudiante_convocatoriaIncorrecta() {

        // Arrange
        estudianteEntity.setIdConvocatoria(99L);

        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.of(estudianteEntity));

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        // Act + Assert
        assertThrows(
                ParticipacionNoEncontradaException.class,
                () -> servicioEstudiante.eliminarEstudiante(1L)
        );
    }

    // =========================================================
    // importarEstudiantes
    // =========================================================

    @Test
    @DisplayName("Debe importar correctamente estudiantes desde un CSV válido")
    void importarEstudiantes_ok() {

        // Arrange
        String csv = """
            CENTRO;Nombre;Apellido1;Apellido2;DNI/NIF;ID_SEDE;DETALLE_MATERIAS
            IES Test;Ana;Ruiz;Gomez;11111111A;1;Matematicas
            """;

        MultipartFile fichero = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csv.getBytes()
        );

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(servicioExternoClient.obtenerTodasMaterias())
                .thenReturn(List.of(
                        new Materia(10L, "Matematicas", false)
                ));

        when(repositorioEstudiante.findByDniAndIdConvocatoria(any(), any()))
                .thenReturn(Optional.empty());

        when(repositorioInstituto.findByNombre(any()))
                .thenReturn(Optional.of(institutoEntity));

        when(repositorioEstudiante.save(any()))
                .thenReturn(estudianteEntity);

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of());

        // Act
        ImportacionEstudiantes resultado =
                servicioEstudiante.importarEstudiantes(fichero);

        // Assert
        assertAll(
                () -> assertEquals(1, resultado.importados().size()),
                () -> assertEquals(0, resultado.noImportados().size())
        );

        verify(repositorioEstudiante).save(any());
    }

    @Test
    @DisplayName("Debe lanzar ViolacionReglaNegocioException cuando el fichero está vacío")
    void importarEstudiantes_ficheroVacio() {

        // Arrange
        MultipartFile fichero = new MockMultipartFile(
                "file",
                new byte[0]
        );

        // Act + Assert
        assertThrows(
                ViolacionReglaNegocioException.class,
                () -> servicioEstudiante.importarEstudiantes(fichero)
        );
    }

    @Test
    @DisplayName("Debe lanzar ViolacionReglaNegocioException cuando el fichero es null")
    void importarEstudiantes_ficheroNull() {

        // Act + Assert
        assertThrows(
                ViolacionReglaNegocioException.class,
                () -> servicioEstudiante.importarEstudiantes(null)
        );
    }

    @Test
    @DisplayName("Debe registrar un estudiante como no importado cuando el DNI ya existe")
    void importarEstudiantes_dniDuplicado() {

        // Arrange
        String csv = """
                CENTRO;Nombre;Apellido1;Apellido2;DNI/NIF;ID_SEDE
                IES Test;Ana;Ruiz;Gomez;11111111A;1
                """;

        MultipartFile fichero = new MockMultipartFile(
                "file",
                csv.getBytes()
        );

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(servicioExternoClient.obtenerTodasMaterias())
                .thenReturn(List.of());

        when(repositorioEstudiante.findByDniAndIdConvocatoria(any(), any()))
                .thenReturn(Optional.of(estudianteEntity));

        // Act
        ImportacionEstudiantes resultado =
                servicioEstudiante.importarEstudiantes(fichero);

        // Assert
        assertEquals(1, resultado.noImportados().size());
    }

    @Test
    @DisplayName("Debe manejar correctamente errores de lectura del CSV")
    void importarEstudiantes_errorLectura() throws IOException {

        // Arrange
        MultipartFile fichero = mock(MultipartFile.class);

        when(fichero.isEmpty()).thenReturn(false);

        when(fichero.getInputStream())
                .thenThrow(new IOException());

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(servicioExternoClient.obtenerTodasMaterias())
                .thenReturn(List.of());

        // Act + Assert
        assertThrows(
                ServicioExternoException.class,
                () -> servicioEstudiante.importarEstudiantes(fichero)
        );
    }

    @Test
    @DisplayName("Debe soportar listas nulas de materias durante la importación")
    void importarEstudiantes_materiasNull() {

        // Arrange
        String csv = """
            CENTRO;Nombre;Apellido1;Apellido2;DNI/NIF;ID_SEDE
            IES Test;Ana;Ruiz;Gomez;11111111A;1
            """;

        MultipartFile fichero = new MockMultipartFile(
                "file",
                csv.getBytes()
        );

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(servicioExternoClient.obtenerTodasMaterias())
                .thenReturn(null);

        when(repositorioEstudiante.findByDniAndIdConvocatoria(any(), any()))
                .thenReturn(Optional.empty());

        when(repositorioInstituto.findByNombre(any()))
                .thenReturn(Optional.of(institutoEntity));

        when(repositorioEstudiante.save(any()))
                .thenReturn(estudianteEntity);

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of());

        // Act
        ImportacionEstudiantes resultado =
                servicioEstudiante.importarEstudiantes(fichero);

        // Assert
        assertEquals(1, resultado.importados().size());
    }

    @Test
    @DisplayName("Debe registrar error cuando el CSV tiene columnas insuficientes")
    void importarEstudiantes_columnasInsuficientes() {

        // Arrange
        String csv = """
                HEADER
                hola
                """;

        MultipartFile fichero = new MockMultipartFile(
                "file",
                csv.getBytes()
        );

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(servicioExternoClient.obtenerTodasMaterias())
                .thenReturn(List.of());

        // Act
        ImportacionEstudiantes resultado =
                servicioEstudiante.importarEstudiantes(fichero);

        // Assert
        assertEquals(1, resultado.noImportados().size());
    }


        @Test
        @DisplayName("Debe lanzar EstudianteNotFoundException al actualizar un estudiante inexistente")
        void actualizarEstudiante_notFound() {

        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EstudianteNotFoundException.class,
                () -> servicioEstudiante.actualizarEstudiante(1L, crearDTO())
        );
        }

        @Test
        @DisplayName("Debe lanzar DniDuplicadoException al actualizar con DNI ya existente")
        void actualizarEstudiante_dniDuplicadoCambiado() {

        estudianteEntity.setDni("OLD");

        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.of(estudianteEntity));

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(repositorioEstudiante.findByDniAndIdConvocatoria("12345678A", 1L))
                .thenReturn(Optional.of(new EstudianteEntity()));

        assertThrows(
                DniDuplicadoException.class,
                () -> servicioEstudiante.actualizarEstudiante(1L, crearDTO())
        );
        }

        @Test
        @DisplayName("Debe lanzar EstudianteNotFoundException al eliminar un estudiante inexistente")
        void eliminarEstudiante_notFound() {

        when(repositorioEstudiante.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EstudianteNotFoundException.class,
                () -> servicioEstudiante.eliminarEstudiante(1L)
        );
        }

        @Test
        @DisplayName("Debe registrar error cuando el instituto del CSV no existe")
        void importarEstudiantes_institutoNoEncontrado() {

        String csv = """
                CENTRO;Nombre;Apellido1;Apellido2;DNI/NIF;ID_SEDE
                IES Inexistente;Ana;Ruiz;Gomez;11111111A;1
                """;

        MultipartFile fichero = new MockMultipartFile("file", csv.getBytes());

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(servicioExternoClient.obtenerTodasMaterias())
                .thenReturn(List.of());

        when(repositorioEstudiante.findByDniAndIdConvocatoria(any(), any()))
                .thenReturn(Optional.empty());

        when(repositorioInstituto.findByNombre("IES Inexistente"))
                .thenReturn(Optional.empty()); 

        ImportacionEstudiantes resultado =
                servicioEstudiante.importarEstudiantes(fichero);

        assertEquals(1, resultado.noImportados().size());
        }

        @Test
        @DisplayName("Debe manejar materias con nombres duplicados en la importación")
        void importarEstudiantes_materiasNombresDuplicados() {

        String csv = """
                CENTRO;Nombre;Apellido1;Apellido2;DNI/NIF;ID_SEDE;DETALLE_MATERIAS
                IES Test;Ana;Ruiz;Gomez;11111111A;1;Matematicas
                """;

        MultipartFile fichero = new MockMultipartFile("file", csv.getBytes());

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(servicioExternoClient.obtenerTodasMaterias())
                .thenReturn(List.of(
                        new Materia(10L, "Matematicas", false),
                        new Materia(11L, "Matematicas", false)
                ));

        when(repositorioEstudiante.findByDniAndIdConvocatoria(any(), any()))
                .thenReturn(Optional.empty());

        when(repositorioInstituto.findByNombre(any()))
                .thenReturn(Optional.of(institutoEntity));

        when(repositorioEstudiante.save(any()))
                .thenReturn(estudianteEntity);

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of());

        ImportacionEstudiantes resultado =
                servicioEstudiante.importarEstudiantes(fichero);

        assertEquals(1, resultado.importados().size());
        }

    @Test
    @DisplayName("Debe ignorar líneas en blanco en el CSV")
    void importarEstudiantes_conLineasBlancas() {

        // Arrange
        String csv = "CENTRO;Nombre;Apellido1;Apellido2;DNI/NIF;ID_SEDE\n" +
                     "\n" + // Línea vacía
                     "IES Test;Ana;Ruiz;Gomez;11111111A;1\n" +
                     "   \n"; // Línea con espacios

        MultipartFile fichero = new MockMultipartFile(
                "file",
                csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        when(servicioExternoClient.obtenerConvocatoriaActual())
                .thenReturn(1L);

        when(servicioExternoClient.obtenerTodasMaterias())
                .thenReturn(List.of());

        when(repositorioEstudiante.findByDniAndIdConvocatoria(any(), any()))
                .thenReturn(Optional.empty());

        when(repositorioInstituto.findByNombre(any()))
                .thenReturn(Optional.of(institutoEntity));

        when(repositorioEstudiante.save(any()))
                .thenReturn(estudianteEntity);

        when(servicioExternoClient.obtenerMateriasBatch(any()))
                .thenReturn(List.of());

        // Act
        ImportacionEstudiantes resultado =
                servicioEstudiante.importarEstudiantes(fichero);

        // Assert
        assertEquals(1, resultado.importados().size());
    }

    @Test
    @DisplayName("Debe ignorar la columna de materias si está vacía o contiene materias desconocidas")
    void importarEstudiantes_materiasInvalidas() {

        // Arrange
        // Línea 2: Columna 7 vacía (Line 409 coverage)
        // Línea 3: Materia que no existe (Line 413 coverage)
        String csv = """
                CENTRO;Nombre;Apellido1;Apellido2;DNI/NIF;ID_SEDE;DETALLE_MATERIAS
                IES Test;Ana;Ruiz;Gomez;11111111A;1;  \s
                IES Test;Pepe;Gonzalez;Sanz;22222222B;1;Inexistente
                """;

        MultipartFile fichero = new MockMultipartFile(
                "file",
                csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        when(servicioExternoClient.obtenerConvocatoriaActual()).thenReturn(1L);
        when(servicioExternoClient.obtenerTodasMaterias()).thenReturn(List.of(new Materia(10L, "Matematicas", false)));
        when(repositorioEstudiante.findByDniAndIdConvocatoria(any(), any())).thenReturn(Optional.empty());
        when(repositorioInstituto.findByNombre(any())).thenReturn(Optional.of(institutoEntity));
        when(repositorioEstudiante.save(any())).thenReturn(estudianteEntity);
        when(servicioExternoClient.obtenerMateriasBatch(any())).thenReturn(List.of());

        // Act
        ImportacionEstudiantes resultado =
                servicioEstudiante.importarEstudiantes(fichero);

        // Assert
        assertEquals(2, resultado.importados().size());
        // Verificamos que se han guardado ambos a pesar de los datos de materias incorrectos
        verify(repositorioEstudiante, times(2)).save(any());
    }

    // =========================================================
    // MÉTODO AUXILIAR
    // =========================================================

    private EstudianteNuevo crearDTO() {

        return new EstudianteNuevo(
                new NombreCompleto("Juan", "Perez", "Lopez"),
                "12345678A",
                "666666666",
                "juan@test.com",
                Set.of(10L),
                1L, // idInstituto
                2L, // idSede
                false
        );
    }
}
