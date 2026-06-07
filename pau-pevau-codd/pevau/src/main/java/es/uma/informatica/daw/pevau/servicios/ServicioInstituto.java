package es.uma.informatica.daw.pevau.servicios;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.uma.informatica.daw.pevau.dtos.Instituto;
import es.uma.informatica.daw.pevau.entidades.InstitutoEntity;
import es.uma.informatica.daw.pevau.excepciones.InstitutoAsociadoException;
import es.uma.informatica.daw.pevau.excepciones.InstitutoNotFoundException;
import es.uma.informatica.daw.pevau.repositorios.RepositorioInstituto;

@Service
@Transactional
public class ServicioInstituto {

    private final RepositorioInstituto repositorioInstituto;

    public ServicioInstituto(RepositorioInstituto repositorioInstituto) {
        this.repositorioInstituto = repositorioInstituto;
    }

    /**
     * Obtiene todos los institutos.
     * @return Una lista con todos los institutos.
     */
    @Transactional(readOnly = true)
    public List<Instituto> obtenerTodos() {
        return repositorioInstituto.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un instituto por su ID.
     * Si el instituto no existe, lanza una excepción InstitutoNotFoundException.
     * @param id El ID del instituto a obtener.
     * @return El instituto encontrado.
     */
    @Transactional(readOnly = true)
    public Instituto obtenerPorId(Long id) {
        InstitutoEntity entity = repositorioInstituto.findById(id)
                .orElseThrow(() -> new InstitutoNotFoundException("Instituto no encontrado: " + id));
        return convertirADTO(entity);
    }

    /**
     * Crea un nuevo instituto a partir de los datos proporcionados en el DTO.
     * @param institutoDTO El DTO con los datos del instituto a crear.
     * @return El instituto creado.
     */
    public Instituto crear(Instituto institutoDTO) {
        InstitutoEntity entity = convertirAEntidad(institutoDTO);
        InstitutoEntity guardado = repositorioInstituto.save(entity);
        return convertirADTO(guardado);
    }

    /**
     * Actualiza un instituto existente con los datos proporcionados en el DTO.
     * Si el instituto no existe, lanza una excepción InstitutoNotFoundException.
     * @param id El ID del instituto a actualizar.
     * @param institutoDTO El DTO con los nuevos datos del instituto.
     * @return El instituto actualizado.
     */
    public Instituto actualizar(Long id, Instituto institutoDTO) {
        InstitutoEntity entity = repositorioInstituto.findById(id)
                .orElseThrow(() -> new InstitutoNotFoundException("Instituto no encontrado: " + id));

        entity.setNombre(institutoDTO.nombre());
        entity.setDireccion1(institutoDTO.direccion1());
        entity.setDireccion2(institutoDTO.direccion2());
        entity.setLocalidad(institutoDTO.localidad());
        entity.setCodigoPostal(institutoDTO.codigoPostal());
        entity.setPais(institutoDTO.pais());

        return convertirADTO(repositorioInstituto.save(entity));
    }

    /**
     * Elimina un instituto existente.
     * Si el instituto no existe, lanza una excepción InstitutoNotFoundException.
     * Si el instituto tiene estudiantes asociados, lanza una excepción InstitutoAsociadoException.
     * @param id El ID del instituto a eliminar.
     */
    public void eliminar(Long id) {
        InstitutoEntity entity = repositorioInstituto.findById(id)
                .orElseThrow(() -> new InstitutoNotFoundException("Instituto no encontrado: " + id));

        // Regla de negocio: Si tiene estudiantes, explota (HTTP 409)
        if (entity.getEstudiantes() != null && !entity.getEstudiantes().isEmpty()) {
            throw new InstitutoAsociadoException("No se puede eliminar el instituto, tiene estudiantes asociados.");
        }

        repositorioInstituto.delete(entity);
    }

    /**
     * Convierte una entidad de Instituto en un DTO de Instituto.
     * @param entity La entidad de Instituto a convertir.
     * @return El DTO de Instituto convertido.
     */
    private Instituto convertirADTO(InstitutoEntity entity) {
        return new Instituto(
                entity.getId(), entity.getNombre(), entity.getDireccion1(),
                entity.getDireccion2(), entity.getLocalidad(),
                entity.getCodigoPostal(), entity.getPais()
        );
    }

    /**
     * Convierte un DTO de Instituto en una entidad de Instituto.
     * @param dto El DTO de Instituto a convertir.
     * @return La entidad de Instituto convertida.
     */
    private InstitutoEntity convertirAEntidad(Instituto dto) {
        InstitutoEntity entity = new InstitutoEntity();
        entity.setNombre(dto.nombre());
        entity.setDireccion1(dto.direccion1());
        entity.setDireccion2(dto.direccion2());
        entity.setLocalidad(dto.localidad());
        entity.setCodigoPostal(dto.codigoPostal());
        entity.setPais(dto.pais());
        return entity;
    }
}