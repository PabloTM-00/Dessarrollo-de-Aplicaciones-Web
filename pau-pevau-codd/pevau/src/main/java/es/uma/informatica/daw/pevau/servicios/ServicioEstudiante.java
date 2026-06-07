package es.uma.informatica.daw.pevau.servicios;

import es.uma.informatica.daw.pevau.dtos.*;
import es.uma.informatica.daw.pevau.entidades.EstudianteEntity;
import es.uma.informatica.daw.pevau.entidades.InstitutoEntity;
import es.uma.informatica.daw.pevau.excepciones.*;
import es.uma.informatica.daw.pevau.repositorios.RepositorioEstudiante;
import es.uma.informatica.daw.pevau.repositorios.RepositorioInstituto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Lógica de negocio para la gestión de estudiantes y sus participaciones en convocatorias.
 * <p>
 * Los datos personales del estudiante (nombre, DNI, etc.) se almacenan en {@code EstudianteEntity}.
 * Los datos específicos de cada convocatoria (sede, materias, noEliminar) se almacenan en
 * {@code ParticipacionEmbeddable} dentro de la misma entidad.
 * </p>
 * <p>
 * Las operaciones de escritura ({@code POST}, {@code PUT}, {@code DELETE}) siempre operan
 * sobre la convocatoria actual, obtenida de {@link ServicioExternoClient}.
 * Las operaciones de lectura ({@code GET}) aceptan un {@code idConvocatoria} opcional;
 * si es {@code null} se usa la convocatoria actual.
 * </p>
 */
@Service
@Transactional
public class ServicioEstudiante {

    private final RepositorioEstudiante repositorioEstudiante;
    private final RepositorioInstituto repositorioInstituto;
    private final ServicioExternoClient servicioExternoClient;

    public ServicioEstudiante(RepositorioEstudiante repositorioEstudiante,
                              RepositorioInstituto repositorioInstituto,
                              ServicioExternoClient servicioExternoClient) {
        this.repositorioEstudiante = repositorioEstudiante;
        this.repositorioInstituto = repositorioInstituto;
        this.servicioExternoClient = servicioExternoClient;
    }

    /**
     * Devuelve la información completa de un estudiante para una convocatoria concreta.
     * Si {@code idConvocatoria} es {@code null} se usa la convocatoria actual.
     *
     * @param id             ID del estudiante.
     * @param idConvocatoria ID de la convocatoria a consultar, o {@code null} para la actual.
     * @return DTO {@link Estudiante} con los datos del estudiante y los de su participación
     *         en la convocatoria indicada.
     * @throws EstudianteNotFoundException            si no existe ningún estudiante con ese ID.
     * @throws ParticipacionNoEncontradaException     si el estudiante no tiene participación en la convocatoria indicada.
     * @throws ConvocatoriaActualNoEncontradaException si {@code idConvocatoria} es {@code null} y no hay convocatoria activa.
     * @throws ServicioExternoException               si falla la comunicación con el servicio de Materias o Convocatorias.
     */
    @Transactional(readOnly = true)
    public Estudiante consultarEstudiante(Long id, Long idConvocatoria) {
        EstudianteEntity estudiante = repositorioEstudiante.findById(id)
                .orElseThrow(() -> new EstudianteNotFoundException(id));

        Long idConvBusqueda = idConvocatoria == null ?
                obtenerConvocatoriaActual() : idConvocatoria;

        // Validamos que el registro encontrado corresponde a la convocatoria solicitada
        if (!estudiante.getIdConvocatoria().equals(idConvBusqueda)) {
            throw new ParticipacionNoEncontradaException(id, idConvBusqueda);
        }

        return convertirAEstudianteDTO(estudiante);
    }

    /**
     * Devuelve la lista de estudiantes que tienen participación en una convocatoria,
     * opcionalmente filtrada por sede.
     * Si {@code idConvocatoria} es {@code null} se usa la convocatoria actual.
     *
     * @param idSede         ID de la sede por la que filtrar, o {@code null} para no filtrar.
     * @param idConvocatoria ID de la convocatoria a consultar, o {@code null} para la actual.
     * @return Lista de DTOs {@link Estudiante} que participan en la convocatoria indicada.
     * @throws ConvocatoriaActualNoEncontradaException si {@code idConvocatoria} es {@code null} y no hay convocatoria activa.
     * @throws ServicioExternoException               si falla la comunicación con servicios externos.
     */
    @Transactional(readOnly = true)
    public List<Estudiante> consultarEstudiantes(Long idSede, Long idConvocatoria) {
        Long convocatoriaId = idConvocatoria != null ? idConvocatoria : obtenerConvocatoriaActual();

        List<EstudianteEntity> entidades;
        if (idSede == null) {
            entidades = repositorioEstudiante.findByIdConvocatoria(convocatoriaId);
        } else {
            entidades = repositorioEstudiante.findByIdConvocatoriaAndIdSede(convocatoriaId, idSede);
        }

        return entidades.stream()
                .map(this::convertirAEstudianteDTO)
                .toList();
    }

    /**
     * Crea un nuevo estudiante con una participación en la convocatoria actual.
     *
     * @param estudianteNuevo DTO con los datos personales y los datos de participación
     *                        (idSede, materiasMatriculadas, noEliminar, idInstituto).
     * @return DTO {@link Estudiante} completo con los objetos Instituto y Materia anidados.
     * @throws ConvocatoriaActualNoEncontradaException si no hay convocatoria activa.
     * @throws DniDuplicadoException                  si ya existe un estudiante con el mismo DNI.
     * @throws InstitutoNotFoundException             si el instituto referenciado no existe.
     * @throws MateriaNotFoundException               si alguno de los IDs de materias no existe en el servicio externo.
     * @throws ViolacionReglaNegocioException         si alguna materia existe pero tiene {@code eliminada = true}.
     * @throws ServicioExternoException               si falla la comunicación con servicios externos.
     */
    public Estudiante crearEstudiante(EstudianteNuevo estudianteNuevo) {
        Long convocatoriaActual = obtenerConvocatoriaActual();

        // Se comprueba por DNI + Convocatoria
        repositorioEstudiante.findByDniAndIdConvocatoria(estudianteNuevo.dni(), convocatoriaActual)
                .ifPresent(s -> { throw new DniDuplicadoException(estudianteNuevo.dni()); });

        validarInstituto(estudianteNuevo.idInstituto());
        validarMaterias(estudianteNuevo.materiasMatriculadas());

        InstitutoEntity instituto = repositorioInstituto.getReferenceById(estudianteNuevo.idInstituto());
        NombreCompleto nombre = estudianteNuevo.nombreCompleto();

        EstudianteEntity estudiante = new EstudianteEntity()
                .setNombre(nombre.nombre())
                .setApellido1(nombre.apellido1())
                .setApellido2(nombre.apellido2())
                .setDni(estudianteNuevo.dni())
                .setTelefono(estudianteNuevo.telefono())
                .setEmail(estudianteNuevo.email())
                .setInstituto(instituto)
                .setIdConvocatoria(convocatoriaActual)
                .setIdSede(estudianteNuevo.idSede())
                .setMateriasMatriculadas(new HashSet<>(estudianteNuevo.materiasMatriculadas()))
                .setNoEliminar(false);

        repositorioEstudiante.save(estudiante);

        return convertirAEstudianteDTO(estudiante);
    }

    /**
     * Actualiza los datos personales del estudiante y su participación en la convocatoria actual.
     * Si el estudiante no tiene participación en la convocatoria actual, se crea una nueva.
     * <p>
     * Regla {@code noEliminar}: solo puede cambiar de {@code false} a {@code true}.
     * Intentar cambiarlo de {@code true} a {@code false} lanza {@link ViolacionReglaNegocioException}.
     * </p>
     *
     * @param id              ID del estudiante a actualizar.
     * @param estudianteNuevo DTO con los datos actualizados.
     * @return DTO {@link Estudiante} completo con los objetos Instituto y Materia anidados.
     * @throws EstudianteNotFoundException            si no existe ningún estudiante con ese ID.
     * @throws ConvocatoriaActualNoEncontradaException si no hay convocatoria activa.
     * @throws DniDuplicadoException                  si el nuevo DNI ya pertenece a otro estudiante.
     * @throws InstitutoNotFoundException             si el instituto referenciado no existe.
     * @throws MateriaNotFoundException               si alguno de los IDs de materias no existe en el servicio externo.
     * @throws ViolacionReglaNegocioException         si se intenta cambiar {@code noEliminar} de {@code true} a {@code false},
     *                                                o si alguna materia tiene {@code eliminada = true}.
     * @throws ServicioExternoException               si falla la comunicación con servicios externos.
     */
    public Estudiante actualizarEstudiante(Long id, EstudianteNuevo estudianteNuevo) {
        EstudianteEntity estudiante = repositorioEstudiante.findById(id)
                .orElseThrow(() -> new EstudianteNotFoundException(id));

        // Regla: Solo operamos sobre la convocatoria actual para actualizaciones
        Long convocatoriaActual = obtenerConvocatoriaActual();
        if (!estudiante.getIdConvocatoria().equals(convocatoriaActual)) {
            throw new ViolacionReglaNegocioException("Solo se pueden actualizar registros de la convocatoria actual.");
        }

        // Comprobar DNI duplicado en ESTA convocatoria
        if (!estudiante.getDni().equals(estudianteNuevo.dni())) {
            repositorioEstudiante.findByDniAndIdConvocatoria(estudianteNuevo.dni(), convocatoriaActual)
                    .ifPresent(s -> { throw new DniDuplicadoException(estudianteNuevo.dni()); });
        }

        validarInstituto(estudianteNuevo.idInstituto());
        validarMaterias(estudianteNuevo.materiasMatriculadas());

        // Regla de negocio: noEliminar
        if (estudiante.isNoEliminar() && !estudianteNuevo.noEliminar()) {
            throw new ViolacionReglaNegocioException("No se puede revertir el estado de 'noEliminar' a false.");
        }

        InstitutoEntity instituto = repositorioInstituto.getReferenceById(estudianteNuevo.idInstituto());
        NombreCompleto nombre = estudianteNuevo.nombreCompleto();

        estudiante.setNombre(nombre.nombre())
                .setApellido1(nombre.apellido1())
                .setApellido2(nombre.apellido2())
                .setDni(estudianteNuevo.dni())
                .setTelefono(estudianteNuevo.telefono())
                .setEmail(estudianteNuevo.email())
                .setInstituto(instituto)
                .setIdSede(estudianteNuevo.idSede())
                .setMateriasMatriculadas(new HashSet<>(estudianteNuevo.materiasMatriculadas()))
                .setNoEliminar(estudianteNuevo.noEliminar());

        return convertirAEstudianteDTO(repositorioEstudiante.save(estudiante));
    }

    /**
     * Elimina la participación del estudiante en la convocatoria actual.
     * Si tras la eliminación el estudiante no tiene ninguna participación restante,
     * se elimina también el registro completo del estudiante.
     *
     * @param id ID del estudiante.
     * @throws EstudianteNotFoundException            si no existe ningún estudiante con ese ID.
     * @throws ConvocatoriaActualNoEncontradaException si no hay convocatoria activa.
     * @throws ParticipacionNoEncontradaException     si el estudiante no tiene participación en la convocatoria actual.
     * @throws ViolacionReglaNegocioException         si la participación tiene {@code noEliminar = true}.
     * @throws ServicioExternoException               si falla la comunicación con el servicio de Convocatorias.
     */
    public void eliminarEstudiante(Long id) {
        EstudianteEntity estudiante = repositorioEstudiante.findById(id)
                .orElseThrow(() -> new EstudianteNotFoundException(id));

        Long idConvocatoriaActual = obtenerConvocatoriaActual();

        if (!estudiante.getIdConvocatoria().equals(idConvocatoriaActual)) {
            throw new ParticipacionNoEncontradaException(id, idConvocatoriaActual);
        }

        if (estudiante.isNoEliminar()) {
            throw new ViolacionReglaNegocioException("Participación con noEliminar = true");
        }

        repositorioEstudiante.delete(estudiante);
    }

    /**
     * Importa estudiantes desde un archivo CSV para la convocatoria actual.
     * Procesa cada línea de forma independiente: los errores en un registro no revierten
     * los registros ya importados correctamente.
     * <p>
     * Para cada registro del CSV:
     * <ul>
     *   <li>Si el estudiante no existe, se crea con una nueva participación.</li>
     *   <li>Si el estudiante ya existe pero no tiene participación en la convocatoria actual,
     *       se añade la participación.</li>
     *   <li>Si ya tiene participación en la convocatoria actual, se registra como error.</li>
     * </ul>
     * </p>
     *
     * @param fichero Archivo CSV con los datos de los estudiantes a importar.
     * @return DTO {@link ImportacionEstudiantes} con las listas de importados y no importados.
     * @throws ConvocatoriaActualNoEncontradaException si no hay convocatoria activa.
     * @throws ServicioExternoException               si falla la comunicación con servicios externos.
     * 
     * Ejemplo del formato de fichero CSV aceptado:
     * 
     * CENTRO;Nombre;Apellido1;Apellido2;DNI/NIF;ID_SEDE;DETALLE_MATERIAS
     * IES Test;Ana;Ruiz;Gómez;12345678A;1;Historia de España, Lengua Castellana y Literatura
     * IES Test;Pablo;Martín;Sanz;23456789B;1;Matemáticas II, Física
     * IES Test;Sofía;Jiménez;Vega;34567890C;1;Biología, Química
     * 
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImportacionEstudiantes importarEstudiantes(MultipartFile fichero) {
        if (fichero == null || fichero.isEmpty()) {
            throw new ViolacionReglaNegocioException("The uploaded file is empty or missing.");
        }

        Long idConvActual = obtenerConvocatoriaActual();
        List<Estudiante> importados = new ArrayList<>();
        List<ProblemaImportacion> noImportados = new ArrayList<>();

        List<Materia> materiasBatch = servicioExternoClient.obtenerTodasMaterias();
        Map<String, Long> mapaMaterias = (materiasBatch == null ? new ArrayList<Materia>() : materiasBatch)
                .stream().collect(Collectors.toMap(
                        m -> m.nombre().trim().toLowerCase(),
                        Materia::id,
                        (id1, id2) -> id1 // Por si hay nombres duplicados
                ));

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(fichero.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            boolean esCabecera = true;
            while ((linea = reader.readLine()) != null) {
                if (esCabecera) { esCabecera = false; continue; }
                if (linea.isBlank()) continue;

                try {
                    Estudiante dto = procesarFila(linea, idConvActual, mapaMaterias);
                    importados.add(dto);
                } catch (DniDuplicadoException e) {
                    noImportados.add(new ProblemaImportacion(null, "El estudiante ya participa en la convocatoria actual: " + e.getMessage()));
                } catch (Exception e) {
                    noImportados.add(new ProblemaImportacion(null, "Error en línea [" + linea + "]: " + e.getMessage()));
                }
            }
        } catch (IOException e) {
            // Workaround raro para evitar cambiar la signature
            throw new ServicioExternoException("Error al leer el archivo CSV", e);
        }

        return new ImportacionEstudiantes(importados, noImportados);
    }

    // -------------------------------------------------------------------------
    // Métodos privados de apoyo
    // -------------------------------------------------------------------------

    /**
     * Obtiene el ID de la convocatoria actual del servicio externo.
     *
     * @return ID de la convocatoria actual.
     * @throws ConvocatoriaActualNoEncontradaException si no hay convocatoria activa.
     * @throws ServicioExternoException               si falla la comunicación con el servicio de Convocatorias.
     */
    private Long obtenerConvocatoriaActual() {
        return servicioExternoClient.obtenerConvocatoriaActual();
    }

    /**
     * Verifica que todas las materias del conjunto existan y no estén eliminadas.
     *
     * @param idsMaterias Conjunto de IDs de materias a validar.
     * @throws MateriaNotFoundException       si alguna materia no existe en el servicio externo.
     * @throws ViolacionReglaNegocioException si alguna materia existe pero tiene {@code eliminada = true}.
     * @throws ServicioExternoException       si falla la comunicación con el servicio de Materias.
     */
    private void validarMaterias(Set<Long> idsMaterias) {
        List<Materia> materias = servicioExternoClient.obtenerMateriasBatch(idsMaterias);
        for (Materia materia : materias) {
            if (materia.eliminada()) {
                throw new ViolacionReglaNegocioException("Materia con ID " + materia.id() + " tiene eliminada = true");
            }
        }
    }

    /**
     * Verifica que el instituto exista en la base de datos propia.
     *
     * @param idInstituto ID del instituto a verificar.
     * @throws InstitutoNotFoundException si no existe ningún instituto con ese ID.
     */
    private void validarInstituto(Long idInstituto) {
        if (!repositorioInstituto.existsById(idInstituto)) {
            throw new InstitutoNotFoundException(String.valueOf(idInstituto));
        }
    }

    /**
     * Convierte una entidad estudiante y su participación en una convocatoria al DTO de respuesta,
     * enriqueciendo con los objetos {@link Materia} obtenidos del servicio externo.
     *
     * @param entity        Entidad del estudiante.
     * @return DTO {@link Estudiante} completo con Instituto y Materias anidados.
     * @throws ServicioExternoException si falla la comunicación con el servicio de Materias.
     */
    private Estudiante convertirAEstudianteDTO(EstudianteEntity entity) {
        List<Materia> materias = servicioExternoClient.obtenerMateriasBatch(entity.getMateriasMatriculadas());
        InstitutoEntity inst = entity.getInstituto();

        Instituto institutoDto = new Instituto(
                inst.getId(), inst.getNombre(), inst.getDireccion1(),
                inst.getDireccion2(), inst.getLocalidad(), inst.getCodigoPostal(), inst.getPais()
        );

        return new Estudiante(
                entity.getId(),
                new NombreCompleto(entity.getApellido1(), entity.getApellido2(), entity.getNombre()),
                entity.getDni(),
                entity.getTelefono(),
                entity.getEmail(),
                new HashSet<>(materias),
                entity.getIdSede(),
                institutoDto,
                entity.isNoEliminar() // Respondemos a tu duda: Aquí va el booleano de la entidad
        );
    }

    private Estudiante procesarFila(String linea, Long idConv, Map<String, Long> mapaMaterias) {
        // Usamos limit -1 para no descartar columnas vacías al final y evitar ArrayIndexOutOfBoundsException
        String[] campos = linea.split(";", -1);
        
        if (campos.length < 6) throw new IllegalArgumentException("Columnas insuficientes (se esperan al menos 6: Centro;Nombre;Apellido1;Apellido2;DNI;ID_SEDE)");

        String nombreCentro = campos[0].trim();
        String nombre = campos[1].trim();
        String apellido1 = campos[2].trim();
        String apellido2 = campos[3].trim();
        String dni = campos[4].trim();
        Long idSede = Long.parseLong(campos[5].trim());

        // -- Verificaciones --
        repositorioEstudiante.findByDniAndIdConvocatoria(dni, idConv)
                .ifPresent(s -> { throw new DniDuplicadoException(dni); });

        // 2. Mapear Centro -> InstitutoEntity
        InstitutoEntity instituto = repositorioInstituto.findByNombre(nombreCentro)
                .orElseThrow(() -> new InstitutoNotFoundException(nombreCentro));

        // 3. Mapear Materias (Nombres -> IDs)
        Set<Long> materiasIds = new HashSet<>();
        if (campos.length > 6 && !campos[6].isBlank()) {
            String[] nombresMaterias = campos[6].split(",");
            for (String n : nombresMaterias) {
                Long idMateria = mapaMaterias.get(n.trim().toLowerCase());
                if (idMateria != null) materiasIds.add(idMateria);
            }
        }

        // 4. Crear Entidad (Datos que faltan en el CSV se ponen por defecto)
        EstudianteEntity entidad = new EstudianteEntity()
                .setNombre(nombre)
                .setApellido1(apellido1)
                .setApellido2(apellido2)
                .setDni(dni)
                .setTelefono("N/A") // No viene en el CSV
                .setEmail(dni.toLowerCase() + "@uma.es") // Placeholder
                .setInstituto(instituto)
                .setIdConvocatoria(idConv)
                .setIdSede(idSede)
                .setMateriasMatriculadas(materiasIds)
                .setNoEliminar(false);

        return convertirAEstudianteDTO(repositorioEstudiante.save(entidad));
    }
}
