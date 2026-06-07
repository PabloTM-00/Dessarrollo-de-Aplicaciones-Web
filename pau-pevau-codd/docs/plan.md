# Plan de implementación del microservicio "Gestión de Estudiantes"

## 1. Análisis de las peticiones API y decisiones de diseño

### 1.1. GET /institutos/{idInstituto}
Propósito: Obtener información de un instituto concreto

Decisión de diseño:
- Almacenamiento: Entidad JPA InstitutoEntity en la propia BD (objeto completo)
- DTO para respuesta: Instituto
- Lógica: Búsqueda directa por ID en la tabla instituto
- Nota: Los institutos son entidades independientes gestionadas a través de endpoints específicos

### 1.2. PUT /institutos/{idInstituto}
Propósito: Actualizar la información de un instituto

Decisión de diseño:
- Almacenamiento: Entidad JPA InstitutoEntity
- DTO de petición y respuesta: Instituto
- Lógica: Búsqueda del registro existente, actualización de campos, guardado en BD
- Nota sobre especificación: La operationId en la spec es "actualizarMateria", lo cual es casi con certeza un error de copia-pega. Se implementará como "actualizarInstituto" en el código, documentando esta discrepancia en la sección de preguntas sobre especificaciones.

### 1.3. DELETE /institutos/{idInstituto}
Propósito: Eliminar un instituto (solo si no tiene estudiantes asociados)

Decisión de diseño:
- Almacenamiento: Entidad JPA InstitutoEntity con relación uno-a-muchos con EstudianteEntity
- Lógica: Verificar si existen estudiantes con instituto.id = idInstituto. Si existen → retornar HTTP 409 Conflict
- Manejo de concurrencia: Se utilizará @Transactional con aislamiento SERIALIZABLE o bloqueo pesimista (LockModeType.PESSIMISTIC_WRITE) para evitar condiciones de carrera donde un estudiante sea creado después de la verificación pero antes de la eliminación. El flujo será: (1) bloquear la fila del instituto, (2) verificar estudiantes asociados, (3) eliminar si procede.

### 1.4. GET /institutos
Propósito: Obtener la lista de todos los institutos

Decisión de diseño:
- Almacenamiento: Entidad JPA InstitutoEntity
- DTO para respuesta: Lista de Instituto
- Lógica: Consultar todos los registros de la tabla instituto

### 1.5. POST /institutos
Propósito: Crear un nuevo instituto

Decisión de diseño:
- Almacenamiento: Entidad JPA InstitutoEntity
- DTO de petición y respuesta: Instituto
- Lógica: Guardar un nuevo registro en la BD con generación automática de ID, retornar el objeto creado y la cabecera Location con la URI del nuevo recurso

### 1.6. GET /estudiantes/{idEstudiante}
Propósito: Obtener información completa de un estudiante (con objetos Instituto y Materia anidados)

Decisión de diseño:
- Almacenamiento: 
  - Entidad JPA EstudianteEntity almacena referencia a InstitutoEntity (relación muchos-a-uno)
  - EstudianteEntity almacena una lista de ParticipacionEmbeddable, cada una con idConvocatoria, idSede, noEliminar (por convocatoria) y una lista materiasMatriculadas
- DTO para respuesta: Estudiante (contiene objetos anidados Instituto y Set de Materia, más idSede y noEliminar)
- Lógica:
  1. Buscar EstudianteEntity por ID
  2. Determinar la convocatoria a consultar: si se recibe parámetro query "idConvocatoria" se usa ese; si no, se consulta al servicio Convocatorias para obtener la convocatoria actual (GET /convocatorias/actual). Si no hay convocatoria actual, se lanza excepción EntidadNoEncontradaException con mensaje "No hay convocatoria activa"
  3. Buscar dentro de la lista de participaciones del estudiante la que coincida con idConvocatoria
  4. Si no existe, retornar HTTP 404
  5. Extraer el InstitutoEntity asociado (de la propia BD) y convertirlo a DTO Instituto
  6. Con los materiasMatriculadas de esa participación, realizar petición (o peteciones) al servicio Materias para obtener todos los objetos Materia. Si alguna materia obtenida del servicio externo tiene eliminada=true, se rechaza la operación lanzando ViolacionReglaNegocioException con mensaje "No se puede matricular en una materia eliminada".
  7. Ensamblar el DTO final Estudiante: usar idSede de la participación, noEliminar de la participación, materiasMatriculadas con los objetos Materia obtenidos


### 1.7. PUT /estudiantes/{idEstudiante}
Propósito: Actualizar la información de un estudiante para la convocatoria actual

Decisión de diseño:
- Almacenamiento: Entidad JPA EstudianteEntity con lista de ParticipacionEmbeddable
- DTO de petición: EstudianteNuevo (contiene idInstituto, materiasMatriculadas como Set de Long, idSede, noEliminar)
- DTO de respuesta: Estudiante (completo, con objetos anidados)
- Regla especial: El campo noEliminar solo puede actualizarse una vez por convocatoria: de false a true. El cambio de true a false está prohibido y debe retornar HTTP 409 Conflict. Esta regla se aplica a la participación de la convocatoria actual.
- Lógica:
  1. Buscar el estudiante existente en la BD
  2. Obtener la convocatoria actual del servicio Convocatorias (GET /convocatorias/actual). Si no existe, lanzar excepción ConvocatoriaActualNoEncontradaException con HTTP 503
  3. Verificar unicidad del DNI (si el DNI cambia)
  4. Verificar existencia de idInstituto en la tabla instituto (BD propia)
  5. Para cada ID en materiasMatriculadas verificar existencia a través del servicio Materias (petición HTTP HEAD o GET).
    - Si alguna materia no existe → lanzar MateriaNotFoundException (HTTP 404)
    - Si alguna materia existe pero tiene eliminada=true → lanzar ViolacionReglaNegocioException (HTTP 409) con mensaje "No se puede matricular en la materia X porque está eliminada"
  6. Buscar la participación del estudiante con idConvocatoria = convocatoria actual
  7. Si no existe, crear una nueva ParticipacionEmbeddable con idConvocatoria = convocatoria actual
  8. Si existe, actualizar sus campos respetando la regla de noEliminar: si el valor actual es false y se recibe true, se actualiza; si el valor actual es true y se recibe false, se lanza excepción ViolacionReglaNegocioException con HTTP 409
  9. Actualizar o crear la participación con los nuevos valores: idSede, materiasMatriculadas, noEliminar
  10. Guardar los cambios en la entidad EstudianteEntity
  11. Retornar el DTO completo (con objetos Instituto y Materia obtenidos), usando la participación actualizada

### 1.8. DELETE /estudiantes/{idEstudiante}
Propósito: Eliminar la participación del estudiante en la convocatoria actual (no elimina al estudiante completo)

Decisión de diseño:
- Almacenamiento: Entidad JPA EstudianteEntity con lista de ParticipacionEmbeddable
- Lógica:
  1. Buscar el estudiante en la BD
  2. Obtener la convocatoria actual del servicio Convocatorias (GET /convocatorias/actual). Si no existe, lanzar excepción ConvocatoriaActualNoEncontradaException con HTTP 503
  3. Buscar la participación con idConvocatoria = convocatoria actual
  4. Si no existe, retornar HTTP 404
  5. Verificar el valor del campo noEliminar de esa participación. Si es true → retornar HTTP 409 Conflict
  6. Si es false, eliminar esa participación de la lista
  7. Si la lista de participaciones queda vacía, eliminar el registro completo del estudiante de la BD
  8. Retornar HTTP 200

### 1.9. GET /estudiantes
Propósito: Obtener la lista filtrada de estudiantes para una convocatoria específica

Decisión de diseño:
- Almacenamiento: Entidad JPA EstudianteEntity con lista de ParticipacionEmbeddable
- Parámetros de consulta:
  - idSede (opcional) — filtro por idSede dentro de la participación de la convocatoria consultada
  - idConvocatoria (opcional, por defecto la actual)
- DTO para respuesta: Lista de Estudiante (cada uno con objetos anidados)
- Lógica:
  1. Determinar idConvocatoria: si se recibe en parámetro, usar ese; si no, consultar a servicio Convocatorias para obtener actual. Si no hay convocatoria actual, lanzar EntidadNoEncontradaException
  2. Consultar todos los estudiantes en la BD
  3. Para cada estudiante, buscar la participación con idConvocatoria = idConvocatoria determinado
  4. Filtrar aquellos estudiantes que tengan participación en esa convocatoria
  5. Si idSede está presente, filtrar adicionalmente por idSede de la participación
  6. Para cada estudiante filtrado, obtener Instituto (de la BD propia) y realizar una única petición batch al servicio Materias para obtener todos los objetos Materia de los materiasMatriculadas de la participación
  7. Ensamblar y retornar la lista final de DTOs Estudiante

### 1.10. POST /estudiantes
Propósito: Crear un nuevo estudiante con una participación en la convocatoria actual

Decisión de diseño:
- Almacenamiento: Entidad JPA EstudianteEntity con lista de ParticipacionEmbeddable
- DTO de petición: EstudianteNuevo (contiene idSede, materiasMatriculadas, noEliminar, sin idConvocatoria)
- DTO de respuesta: Estudiante + cabecera Location con la URI del nuevo recurso
- Lógica:
  1. Obtener la convocatoria actual del servicio Convocatorias (GET /convocatorias/actual). Si no existe, lanzar excepción ConvocatoriaActualNoEncontradaException con HTTP 503
  2. Verificar unicidad del DNI en la tabla estudiante (si ya existe, lanzar excepción DniDuplicadoException con HTTP 409)
  3. Verificar existencia de idInstituto en la tabla instituto (BD propia)
  4. Para cada ID en materiasMatriculadas verificar existencia a través del servicio Materias.
    - Si alguna materia no existe → lanzar MateriaNotFoundException (HTTP 404)
    - Si alguna materia existe pero tiene eliminada=true → lanzar ViolacionReglaNegocioException (HTTP 409) con mensaje "No se puede matricular en la materia X porque está eliminada"
  5. Crear nueva entidad EstudianteEntity con los datos personales (sin idSede, materiasMatriculadas ni noEliminar a nivel de estudiante)
  6. Crear una nueva ParticipacionEmbeddable con: idConvocatoria = convocatoria actual, idSede, materiasMatriculadas, noEliminar
  7. Añadir la participación a la lista del estudiante
  8. Guardar en la BD
  9. Retornar el DTO completo (con objetos Instituto y Materia obtenidos), usando la participación recién creada

### 1.11. POST /estudiantes/upload
Propósito: Importación masiva de estudiantes desde un archivo CSV para la convocatoria actual

Decisión de diseño:
- Almacenamiento: Entidad JPA EstudianteEntity con lista de ParticipacionEmbeddable
- DTO para respuesta: ImportacionEstudiantes (contiene dos listas: importados correctamente y con errores)
- Lógica:
  1. Obtener la convocatoria actual del servicio Convocatorias (GET /convocatorias/actual). Si no existe, lanzar excepción ConvocatoriaActualNoEncontradaException con HTTP 503
  2. Leer el archivo CSV subido
  3. Parsear cada línea a un objeto de tipo EstudianteNuevo (contiene idSede, materiasMatriculadas, noEliminar, etc.)
  4. Para cada registro, ejecutar validaciones:
      - Unicidad del DNI en la BD (considerando estudiantes ya existentes)
      - Existencia del instituto
      - Existencia de las materias
      - Que ninguna materia tenga eliminada=true (consultar servicio Materias)
      - Si el estudiante ya existe, verificar que no tenga ya una participación en la convocatoria actual (para evitar duplicados)
  5. Registros exitosos:
     - Si el estudiante no existe: crear nuevo EstudianteEntity y añadir ParticipacionEmbeddable con convocatoria actual
     - Si el estudiante ya existe pero no tiene participación en convocatoria actual: añadir nueva ParticipacionEmbeddable
     - Guardar en la BD
     - Añadir a la lista importados (como DTO completo Estudiante, construido con esa participación)
  6. Registros con error:
     - Añadir a la lista noImportados junto con el texto del error (objeto ProblemaImportacion usando EstudianteNuevo)
  7. Retornar la respuesta agregada con ambas listas
- Nota sobre rollback: Por especificación, si un registro falla, los registros exitosos previos NO se revierten. Este comportamiento debe documentarse en la API.

## 2. Entidades JPA (Entity)

### 2.1. InstitutoEntity
Entidad completa, almacenada en su propia BD. Contiene los campos: id, nombre, direccion1, direccion2, localidad, codigoPostal, pais. Tiene una relación uno-a-muchos con EstudianteEntity (un instituto puede tener muchos estudiantes).

### 2.2. EstudianteEntity
Entidad principal del microservicio. Almacena:
- Identificador del estudiante (id)
- nombre (String)
- apellido1 (String)
- apellido2 (String)
- dni (String, obligatorio, único)
- telefono (String)
- email (String)
- Relación muchos-a-uno con InstitutoEntity (campo instituto)
- Lista de participaciones: Set<ParticipacionEmbeddable> (anotado con @ElementCollection)

Nota: Los campos idSede, materiasMatriculadas y noEliminar ya no están en EstudianteEntity. Ahora están dentro de cada ParticipacionEmbeddable.

### 2.3. ParticipacionEmbeddable (nueva clase @Embeddable)
Clase embebible que representa la participación de un estudiante en una convocatoria específica:
- idConvocatoria (Long) — identificador de la convocatoria (servicio externo)
- idSede (Long) — identificador de la sede donde se examina (servicio externo)
- noEliminar (Boolean) — indica si esta participación está bloqueada para eliminación
- materiasMatriculadas (Set<Long>) — identificadores de las materias matriculadas (anotado con @ElementCollection)

### 2.4. Relaciones entre entidades
- InstitutoEntity (1) ←→ EstudianteEntity (N): Un instituto puede estar relacionado con muchos estudiantes. Al eliminar un instituto, hay que verificar si tiene estudiantes asociados (considerando cualquier participación de esos estudiantes) usando bloqueo pesimista.
- EstudianteEntity (1) ←→ ParticipacionEmbeddable (N): Composición. Las participaciones se eliminan automáticamente al eliminar el estudiante.
- ParticipacionEmbeddable (1) ←→ Materia (N): No hay relación JPA directa. Se utiliza una colección de identificadores materiasMatriculadas con una tabla auxiliar (generada automáticamente por @ElementCollection).

## 3. DTO (Data Transfer Objects)

### 3.1. Instituto
Se utiliza para todas las operaciones con institutos (GET, POST, PUT). Contiene los mismos campos que InstitutoEntity: id, nombre, direccion1, direccion2, localidad, codigoPostal, pais.

### 3.2. NombreCompleto
DTO anidado según especificación. Contiene los campos:
- nombre (String)
- apellido1 (String)
- apellido2 (String)

### 3.3. EstudianteNuevo
Se utiliza para la creación (POST /estudiantes) y actualización (PUT /estudiantes/{id}) de estudiantes. Contiene:
- nombreCompleto (objeto NombreCompleto)
- dni (String)
- telefono (String)
- email (String)
- materiasMatriculadas (Set de Long) — solo identificadores de materias (para la convocatoria actual)
- idInstituto (Long) — identificador del instituto
- idSede (Long) — identificador de la sede (para la convocatoria actual)
- noEliminar (Boolean) — para la convocatoria actual

Nota: Este DTO NO contiene idConvocatoria. Siempre se refiere a la convocatoria actual (obtenida del servicio externo).

### 3.4. Estudiante
Se utiliza para las respuestas de peticiones GET. Contiene:
- id (Long)
- nombreCompleto (objeto NombreCompleto)
- dni (String)
- telefono (String)
- email (String)
- materiasMatriculadas (Set de Materia) — objetos Materia completos (de la convocatoria recuperada)
- idSede (Long) — de la convocatoria recuperada
- instituto (objeto Instituto)
- noEliminar (Boolean) — de la convocatoria recuperada

Nota: Este DTO NO tiene campo idConvocatoria porque el cliente ya conoce la convocatoria que solicitó (por parámetro query o porque es la actual). Los datos devueltos corresponden siempre a una sola convocatoria.

### 3.5. Materia
DTO obtenido del servicio externo Materias. Contiene los campos: id, nombre, eliminada.

### 3.6. ImportacionEstudiantes
DTO para respuesta de POST /estudiantes/upload. Contiene:
- importados (Lista de Estudiante)
- noImportados (Lista de ProblemaImportacion)

### 3.7. ProblemaImportacion
Nota: Aunque la especificación indica "estudiante: Estudiante", esto es un error lógico. Se implementa con EstudianteNuevo (lo que el cliente envió). Contiene:
- estudiante (objeto EstudianteNuevo)
- problemaImportacion (String)

## 4. Servicios

### 4.1. ServicioInstituto
Propósito: Gestionar la lógica de negocio relacionada con institutos.

Métodos:
- obtenerPorId(Long id): Instituto — Busca un instituto por ID. Lanza InstitutoNotFoundException si no existe.
- obtenerTodos(): List<Instituto> — Retorna todos los institutos.
- crear(Instituto instituto): Instituto — Guarda un nuevo instituto (el ID se genera automáticamente).
- actualizar(Long id, Instituto instituto): Instituto — Actualiza un instituto existente. Lanza InstitutoNotFoundException si no existe.
- eliminar(Long id): void — Elimina un instituto. Lanza InstitutoNotFoundException si no existe. Lanza InstitutoAsociadoException si el instituto tiene estudiantes asociados (considerando cualquier participación de esos estudiantes). Usa bloqueo pesimista para evitar condiciones de carrera.

### 4.2. ServicioEstudiante
Propósito: Gestionar la lógica de negocio relacionada con estudiantes y sus participaciones.

Métodos principales:
- consultarEstudiante(Long id, Long idConvocatoria): Estudiante — Busca un estudiante por ID y convocatoria. Si idConvocatoria es null, usa convocatoria actual. Lanza EstudianteNotFoundException o ParticipacionNoEncontradaException según corresponda.
- consultarEstudiantes(Long idSede, Long idConvocatoria): List<Estudiante> — Filtra estudiantes por sede y convocatoria. Si idConvocatoria es null, usa convocatoria actual.
- crearEstudiante(EstudianteNuevo nuevo): Estudiante — Crea un nuevo estudiante con una participación en la convocatoria actual. Valida DNI único, existencia de instituto y existencia de materias.
- actualizarEstudiante(Long id, EstudianteNuevo actualizado): Estudiante — Actualiza la participación del estudiante en la convocatoria actual. Aplica regla de noEliminar (solo false→true, nunca true→false).
- eliminarEstudiante(Long id): void — Elimina la participación del estudiante en la convocatoria actual. Si no quedan participaciones, elimina el estudiante completo. Valida noEliminar antes de eliminar.
- importarEstudiantes(MultipartFile archivo): ImportacionEstudiantes — Importa estudiantes desde CSV para la convocatoria actual. Procesa línea por línea sin rollback ante fallos.

Métodos privados de apoyo:
- obtenerConvocatoriaActual(): Long — Llama al servicio externo para obtener el ID de la convocatoria actual. Lanza ConvocatoriaActualNoEncontradaException si no existe.
- validarMaterias(Set<Long> idsMaterias): void — Verifica que todas las materias existan y no estén eliminadas en el servicio externo. Lanza MateriaNotFoundException si alguna no existe. Lanza ViolacionReglaNegocioException si alguna existe pero tiene eliminada=true.
- validarInstituto(Long idInstituto): void — Verifica que el instituto exista en la BD propia. Lanza InstitutoNotFoundException si no existe.
- convertirAEstudianteDTO(EstudianteEntity entity, ParticipacionEmbeddable participacion): Estudiante — Convierte entidad + participación a DTO incluyendo llamada al servicio de materias.

### 4.3. ServicioExternoClient (cliente único para servicios externos)
Propósito: Centralizar todas las comunicaciones HTTP con servicios externos (Materias, Convocatorias, Sedes).

Configuración técnica:
- Timeout de conexión: 5 segundos
- Timeout de lectura: 5 segundos
- Reintentos: 3 reintentos con backoff exponencial (100ms, 500ms, 1000ms)
- Circuit Breaker: si falla 5 veces en 30 segundos, abrir circuito durante 60 segundos

Métodos para Convocatorias:
- obtenerConvocatoriaActual(): ConvocatoriaActual — GET /convocatorias/actual. Lanza ConvocatoriaActualNoEncontradaException si el servicio responde 404. Lanza ServicioExternoException por timeouts o errores de conexión.

Métodos para Materias:
- obtenerMateria(Long id): Materia — GET /materias/{id}. Lanza MateriaNotFoundException si responde 404. El DTO Materia incluye el campo eliminada (boolean) que debe ser verificado por el ServicioEstudiante.
- obtenerMateriasBatch(Set<Long> ids): List<Materia> — Opcional: endpoint batch GET /materias?ids=... Si no existe batch, implementa con peticiones paralelas usando CompletableFuture.

Métodos para Sedes (opcional):
- validarSede(Long id): boolean — GET /sedes/{id} para verificar existencia (200 vs 404). Opcional, puede no implementarse si idSede se trata como opaco.

## 5. Controladores

### 5.1. ControladorInstituto
Propósito: Exponer endpoints REST para la gestión de institutos.

Ubicación: controllers/InstitutoController.java
Ruta base: /institutos

Endpoints:
- GET /institutos — Lista todos los institutos. Delega en ServicioInstituto.obtenerTodos().
- GET /institutos/{idInstituto} — Obtiene un instituto por ID. Delega en ServicioInstituto.obtenerPorId(id). Captura InstitutoNotFoundException y retorna HTTP 404.
- POST /institutos — Crea un nuevo instituto. Delega en ServicioInstituto.crear(). Añade cabecera Location con la URI del nuevo recurso.
- PUT /institutos/{idInstituto} — Actualiza un instituto existente. Delega en ServicioInstituto.actualizar(id, instituto). Captura InstitutoNotFoundException y retorna HTTP 404.
- DELETE /institutos/{idInstituto} — Elimina un instituto. Delega en ServicioInstituto.eliminar(id). Captura InstitutoNotFoundException (404) e InstitutoAsociadoException (409).

### 5.2. ControladorEstudiante
Propósito: Exponer endpoints REST para la gestión de estudiantes y sus participaciones.

Ubicación: controllers/EstudianteController.java
Ruta base: /estudiantes

Endpoints:
- GET /estudiantes — Lista estudiantes con filtros opcionales. Parámetros query: idSede (opcional), idConvocatoria (opcional, por defecto convocatoria actual). Delega en ServicioEstudiante.consultarEstudiantes(idSede, idConvocatoria). Captura ConvocatoriaActualNoEncontradaException (503).
- GET /estudiantes/{idEstudiante} — Obtiene un estudiante por ID. Parámetros query: idConvocatoria (opcional, por defecto convocatoria actual). Delega en ServicioEstudiante.consultarEstudiante(id, idConvocatoria). Captura EstudianteNotFoundException (404), ParticipacionNoEncontradaException (404) y ConvocatoriaActualNoEncontradaException (503).
- POST /estudiantes — Crea un nuevo estudiante con participación en convocatoria actual. Delega en ServicioEstudiante.crearEstudiante(). Añade cabecera Location. Captura DniDuplicadoException (409), InstitutoNotFoundException (404), MateriaNotFoundException (404) y ConvocatoriaActualNoEncontradaException (503).
- PUT /estudiantes/{idEstudiante} — Actualiza la participación en convocatoria actual. Delega en ServicioEstudiante.actualizarEstudiante(id, estudianteNuevo). Captura EstudianteNotFoundException (404), ViolacionReglaNegocioException (409), DniDuplicadoException (409), MateriaNotFoundException (404) y ConvocatoriaActualNoEncontradaException (503).
- DELETE /estudiantes/{idEstudiante} — Elimina la participación en convocatoria actual (o el estudiante si no quedan participaciones). Delega en ServicioEstudiante.eliminarEstudiante(id). Captura EstudianteNotFoundException (404), ParticipacionNoEncontradaException (404), ViolacionReglaNegocioException (409) y ConvocatoriaActualNoEncontradaException (503).
- POST /estudiantes/upload — Importa estudiantes desde archivo CSV para la convocatoria actual. Parámetro multipart: ficheroEstudiantes. Delega en ServicioEstudiante.importarEstudiantes(). Captura ConvocatoriaActualNoEncontradaException (503) y otras excepciones de validación.

## 6. Manejo de Errores y Excepciones

### 6.1. Excepciones personalizadas (ya existentes en el proyecto)
- EstudianteNotFoundException - 404 (hereda de EntidadNoEncontradaException)
- InstitutoNotFoundException - 404 (hereda de EntidadNoEncontradaException)
- MateriaNotFoundException - 404 (hereda de EntidadNoEncontradaException)
- DniDuplicadoException - 409
- InstitutoAsociadoException - 409 (hereda de ViolacionReglaNegocioException)
- ViolacionReglaNegocioException - 409 (incluye violación de regla noEliminar en una convocatoria y matricular en materia eliminada)
- ServicioExternoException - 503

### 6.2. Nuevas excepciones a crear
- ConvocatoriaActualNoEncontradaException - 503 (cuando se necesita convocatoria actual y no existe)
- ParticipacionNoEncontradaException - 404 (cuando un estudiante no tiene participación en una convocatoria específica)

### 6.3. ManejadorGlobalExcepciones (Global Exception Handler)
Se implementa un RestControllerAdvice que maneja:
- EstudianteNotFoundException → HTTP 404
- InstitutoNotFoundException → HTTP 404
- ParticipacionNoEncontradaException → HTTP 404
- DniDuplicadoException → HTTP 409
- InstitutoAsociadoException → HTTP 409
- ViolacionReglaNegocioException → HTTP 409
- ConvocatoriaActualNoEncontradaException → HTTP 503
- ServicioExternoException → HTTP 503
- Otras excepciones → HTTP 500

Nota sobre HTTP 403: Se implementa según especificación, devolviendo el mismo schema que la respuesta exitosa (Instituto o Estudiante). En la práctica se usará un DTO de error estándar.


## 7. Anexo: Preguntas sobre especificaciones

## Preguntas para el profesor sobre inconsistencia entre descripción textual y OpenAPI

### 1. Sobre ordenamiento
La descripción textual menciona *"ordenarlos por algunos campos"*, pero la especificación OpenAPI no incluye ningún parámetro de ordenación en `GET /estudiantes`.  
**Pregunta:** ¿Debemos implementar ordenamiento? En caso afirmativo, ¿por qué campos se puede ordenar y cómo se especifica (ej. `?sort=nombre&direction=asc`)?