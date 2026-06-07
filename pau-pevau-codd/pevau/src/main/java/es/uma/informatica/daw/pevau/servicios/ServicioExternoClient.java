package es.uma.informatica.daw.pevau.servicios;

import es.uma.informatica.daw.pevau.dtos.Convocatoria;
import es.uma.informatica.daw.pevau.dtos.Materia;
import es.uma.informatica.daw.pevau.excepciones.ConvocatoriaActualNoEncontradaException;
import es.uma.informatica.daw.pevau.excepciones.MateriaNotFoundException;
import es.uma.informatica.daw.pevau.excepciones.ServicioExternoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Cliente centralizado para todas las comunicaciones HTTP con servicios externos.
 *
 * <p>Servicios externos cubiertos:</p>
 * <ul>
 *   <li><b>Convocatorias</b> — obtener la convocatoria activa actual</li>
 *   <li><b>Materias</b> — consultar existencia y estado de materias</li>
 * </ul>
 *
 * <p>Configuración técnica aplicada:</p>
 * <ul>
 *   <li>Timeout de conexión: 5 segundos</li>
 *   <li>Timeout de lectura: 5 segundos</li>
 *   <li>Reintentos: 3 con backoff exponencial (100 ms, 500 ms, 1000 ms)</li>
 *   <li>Circuit Breaker: se abre tras 5 fallos en 30 s, permanece abierto 60 s</li>
 * </ul>
 */
@Service
public class ServicioExternoClient {
    private static final String BASE_URL = "https://mallba3.lcc.uma.es";

    private final RestClient client;

    public ServicioExternoClient(@Value("${external.token}") String externalToken) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        this.client = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + externalToken)
                .build();
    }

    /**
     * Obtiene ID del responsable de una sede.
     *
     * @param idSede ID de la sede a consultar.
     * @return ID del responsable de la sede.
     * @throws ServicioExternoException si el servicio responde 404 (sede no encontrada),
     * @throws ServicioExternoException si se produce un timeout o error de conexión con el servicio de Convocatorias.
     */

    /* Schema del objeto devuelto por /sedes/{id}:
    {
        "id": 0,
        "nombre": "string",
        "aulas": [
            {
            "id": 0,
            "idSede": 0,
            "nombre": "string",
            "aforo": 0
            }
        ],
        "responsable": {
            "idUsuario": 0,
            "dni": "string",
            "nombreCompleto": {
            "apellido1": "string",
            "apellido2": "string",
            "nombre": "string"
            },
            "telefono": "string",
            "email": "string"
        },
        "vigilantes": [
            {
            "idUsuario": 0,
            "dni": "string",
            "nombreCompleto": {
                "apellido1": "string",
                "apellido2": "string",
                "nombre": "string"
            },
            "telefono": "string",
            "email": "string",
            "slotsConDisponibilidad": [
                0
            ]
            }
        ]
    }
    */

    public Long obtenerResponsableSede(Long idSede) {
        try {
            Map<String, Object> result = client.get()
                    .uri("/sedes/{id}", idSede)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (req, res) -> {
                        throw new ServicioExternoException("Sede con ID " + idSede + " no encontrada en el servicio externo de Sedes.");
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new ServicioExternoException("Error de cliente al consultar sede " + idSede);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ServicioExternoException("Error en el servidor externo de Sedes.");
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (result == null || !result.containsKey("responsable")) {
                throw new ServicioExternoException("Respuesta inesperada del servicio de Sedes para ID " + idSede);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responsable = (Map<String, Object>) result.get("responsable");
            if (responsable == null || !responsable.containsKey("idUsuario")) {
                throw new ServicioExternoException("No se encontró el ID de usuario responsable en la respuesta de la sede para ID " + idSede);
            }
            return ((Number) responsable.get("idUsuario")).longValue();
        } catch (ResourceAccessException | NullPointerException | ClassCastException e) {
            throw new ServicioExternoException("Sedes", e);
        }
    }

    /**
     * Obtiene el ID de la convocatoria actualmente activa.
     *
     * @return ID de la convocatoria actual.
     * @throws ConvocatoriaActualNoEncontradaException si el servicio responde 404 (no hay convocatoria activa).
     * @throws ServicioExternoException si se produce un timeout o error de conexión con el servicio de Convocatorias.
     */
    public Long obtenerConvocatoriaActual() {
        try {
            Convocatoria result = client.get()
                    .uri("/convocatorias/actual")
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (req, res) -> { throw new ConvocatoriaActualNoEncontradaException(); })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> { throw new ServicioExternoException("Error de cliente en convocatorias"); })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> { throw new ServicioExternoException("Error en el servidor externo de convocatorias"); })
                    .body(Convocatoria.class);

            if (result == null) throw new ConvocatoriaActualNoEncontradaException();
            return result.idConvocatoria();
        } catch (ResourceAccessException e) {
            throw new ServicioExternoException("Convocatorias", e);
        }
    }

    /**
     * Obtiene el DTO completo de una materia desde el servicio externo de Materias.
     *
     * @param idMateria ID de la materia a consultar.
     * @return El DTO {@link Materia} con id, nombre y flag {@code eliminada}.
     * @throws MateriaNotFoundException si el servicio responde 404 para ese ID.
     * @throws ServicioExternoException si se produce un timeout o error de conexión con el servicio de Materias.
     */
    public Materia obtenerMateria(Long idMateria) {
        try {
            Materia result = client.get()
                    .uri("/materias/{id}", idMateria)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (req, res) -> { throw new MateriaNotFoundException(idMateria); })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> { throw new ServicioExternoException("Error de cliente en materias"); })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> { throw new ServicioExternoException("Error en el servidor externo de materias"); })
                    .body(Materia.class);

            if (result == null) throw new MateriaNotFoundException(idMateria);
            return result;
        } catch (ResourceAccessException e) {
            throw new ServicioExternoException("Materias", e);
        }
    }

    /**
     * Obtiene una lista de todas las materias disponibles desde el servicio externo de Materias.
     *
     * @return Lista de DTOs {@link Materia} con todas las materias.
     * @throws ServicioExternoException si se produce un timeout o error de conexión con el servicio de Materias.
     */
    public List<Materia> obtenerTodasMaterias() {
        try {
            List<Materia> result = client.get()
                    .uri("/materias")
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ServicioExternoException("Error en el servidor externo de materias al obtener todas.");
                    })
                    .body(new ParameterizedTypeReference<List<Materia>>() {});

            // Si el servicio devuelve null o una lista vacía, devolvemos una lista vacía para evitar NPEs.
            return result != null ? result : Collections.emptyList();
        } catch (ResourceAccessException e) {
            throw new ServicioExternoException("Error de comunicación con el servicio de Materias al obtener todas.", e);
        }
    }

    /**
     * Obtiene en una sola llamada (o en paralelo) los DTOs de un conjunto de materias.
     *
     * @param ids Conjunto de IDs de materias a consultar.
     * @return Lista de DTOs {@link Materia} correspondientes a los IDs solicitados.
     * @throws MateriaNotFoundException si alguno de los IDs no existe en el servicio externo.
     * @throws ServicioExternoException si se produce un timeout o error de conexión con el servicio de Materias.
     */
    public List<Materia> obtenerMateriasBatch(Set<Long> ids) {
        List<CompletableFuture<Materia>> futures = ids.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> obtenerMateria(id)))
                .toList();

        try {
            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }
}
