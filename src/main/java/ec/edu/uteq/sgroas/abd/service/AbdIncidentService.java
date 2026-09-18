package ec.edu.uteq.sgroas.abd.service;

import ec.edu.uteq.sgroas.abd.dto.AbdDtos;
import ec.edu.uteq.sgroas.abd.entity.Alert;
import ec.edu.uteq.sgroas.abd.entity.AbdIncident;
import ec.edu.uteq.sgroas.abd.entity.Unit;
import ec.edu.uteq.sgroas.abd.repository.AlertRepository;
import ec.edu.uteq.sgroas.abd.repository.AbdIncidentRepository;
import ec.edu.uteq.sgroas.abd.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AbdIncidentService {

    private final AbdIncidentRepository incidenteRepository;
    private final AlertRepository alertaRepository;
    private final UnitRepository unidadRepository;

    /**
     * Recupera los incidentes registrados aplicando filtros opcionales de forma paginada.
     * @param estado estado por el que se desea filtrar los incidentes, puede ser nulo para no filtrar.
     * @param nivel nivel de riesgo sugerido por el que se desea filtrar, puede ser nulo para no filtrar.
     * @param search texto para buscar coincidencias en los datos del incidente, puede ser nulo para no filtrar.
     * @param pageable configuracion de paginacion y orden solicitada por el cliente.
     * @return pagina con los datos resumidos de los incidentes encontrados.
     */
    @Transactional(readOnly = true)
    public Page<AbdDtos.AbdIncidentResponse> list(String estado, String nivel, String search, Pageable pageable) {
        String estadoFiltro = (estado == null || estado.isBlank()) ? null : estado.trim().toLowerCase();
        String nivelFiltro = (nivel == null || nivel.isBlank()) ? null : nivel.trim().toLowerCase();
        String searchFiltro = (search == null || search.isBlank()) ? null : search.trim().toLowerCase();
        if (estadoFiltro == null && nivelFiltro == null && searchFiltro == null) {
            return incidenteRepository.findAll(pageable).map(this::aResponse);
        }
        if (searchFiltro != null) {
            return incidenteRepository
                    .searchWithFilters(estadoFiltro, nivelFiltro, searchFiltro,
                            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
                    .map(this::aResponse);
        }
        Page<AbdIncident> page;
        if (estadoFiltro != null) {
            page = incidenteRepository.findByStatusIgnoreCase(estadoFiltro, pageable);
        } else {
            page = incidenteRepository.findBySuggestedLevelIgnoreCase(nivelFiltro, pageable);
        }
        return page.map(this::aResponse);
    }

    /**
     * Registra un incidente asociado a una unidad y genera una alerta cuando el riesgo es alto.
     * @param request datos del incidente reportado junto con la unidad involucrada.
     * @return datos resumidos del incidente guardado.
     * @throws IllegalArgumentException cuando la unidad indicada no existe en el sistema.
     */
    public AbdDtos.AbdIncidentResponse create(AbdDtos.AbdIncidentRequest request) {
        Unit unidad = unidadRepository.findById(request.idUnidad())
                .orElseThrow(() -> new IllegalArgumentException("Unit no encontrada: " + request.idUnidad()));
        AbdIncident incidente = AbdIncident.builder()
                .tipo(request.tipo())
                .descripcion(request.descripcion())
                .nivelSugerido(request.nivelSugerido())
                .evidencia(request.evidencia())
                .estado(request.estado() == null ? "Reportado" : request.estado())
                .unidad(unidad)
                .build();
        incidente = incidenteRepository.save(incidente);

        if ("ALTO".equalsIgnoreCase(incidente.getNivelSugerido())) {
            generateAlert(incidente);
        }
        return aResponse(incidente);
    }

    /**
     * Modifica los datos de un incidente existente y actualiza la unidad involucrada.
     * @param idIncidente identificador del incidente que se desea modificar.
     * @param request nuevos datos del incidente que reemplazaran a los actuales.
     * @return datos resumidos del incidente actualizado.
     * @throws IllegalArgumentException cuando el incidente o la unidad indicada no existen en el sistema.
     */
    public AbdDtos.AbdIncidentResponse update(Integer idIncidente, AbdDtos.AbdIncidentRequest request) {
        AbdIncident incidente = incidenteRepository.findById(idIncidente)
                .orElseThrow(() -> new IllegalArgumentException("Incident no encontrado: " + idIncidente));
        Unit unidad = unidadRepository.findById(request.idUnidad())
                .orElseThrow(() -> new IllegalArgumentException("Unit no encontrada: " + request.idUnidad()));
        incidente.setTipo(request.tipo());
        incidente.setDescripcion(request.descripcion());
        incidente.setNivelSugerido(request.nivelSugerido());
        if (request.evidencia() != null) {
            incidente.setEvidencia(request.evidencia());
        }
        if (request.estado() != null) {
            incidente.setEstado(request.estado());
        }
        incidente.setUnidad(unidad);
        return aResponse(incidenteRepository.save(incidente));
    }

    /**
     * Suprime del sistema el registro de un incidente existente.
     * @param idIncidente identificador del incidente que se desea suprimir.
     * @throws IllegalArgumentException cuando no existe un incidente con el identificador indicado.
     */
    public void delete(Integer idIncidente) {
        if (!incidenteRepository.existsById(idIncidente)) {
            throw new IllegalArgumentException("Incident no encontrado: " + idIncidente);
        }
        incidenteRepository.deleteById(idIncidente);
    }

    private void generateAlert(AbdIncident incidente) {
        Alert alerta = Alert.builder()
                .nivelRiesgo("ALTO")
                .descripcion(incidente.getDescripcion())
                .incidente(incidente)
                .build();
        alertaRepository.save(alerta);
    }

    private AbdDtos.AbdIncidentResponse aResponse(AbdIncident i) {
        return new AbdDtos.AbdIncidentResponse(
                i.getIdIncidente(), i.getTipo(), i.getDescripcion(),
                i.getNivelSugerido(), i.getFechaIncidente().toString(), i.getEvidencia(),
                i.getEstado(), i.getUnidad().getIdUnidad(), i.getUnidad().getPlaca()
        );
    }
}
