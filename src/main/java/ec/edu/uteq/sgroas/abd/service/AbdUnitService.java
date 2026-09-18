package ec.edu.uteq.sgroas.abd.service;

import ec.edu.uteq.sgroas.abd.dto.AbdDtos;
import ec.edu.uteq.sgroas.abd.entity.Unit;
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
public class AbdUnitService {

    private final UnitRepository unidadRepository;

    /**
     * Recupera las unidades registradas aplicando filtros opcionales de forma paginada.
     * @param estado estado por el que se desea filtrar las unidades, puede ser nulo para no filtrar.
     * @param search texto para buscar coincidencias en placa, disco o modelo, puede ser nulo para no filtrar.
     * @param pageable configuracion de paginacion y orden solicitada por el cliente.
     * @return pagina con los datos resumidos de las unidades encontradas.
     */
    @Transactional(readOnly = true)
    public Page<AbdDtos.UnitResponse> list(String estado, String search, Pageable pageable) {
        String estadoFiltro = (estado == null || estado.isBlank()) ? null : estado.trim().toLowerCase();
        String searchFiltro = (search == null || search.isBlank()) ? null : search.trim().toLowerCase();
        Page<Unit> page;
        if (estadoFiltro == null && searchFiltro == null) {
            page = unidadRepository.findAll(pageable);
        } else {
            page = unidadRepository.searchWithFilters(estadoFiltro, searchFiltro,
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
        }
        return page.map(this::aResponse);
    }

    /**
     * Obtiene el detalle de una unidad existente a partir de su identificador.
     * @param idUnidad identificador de la unidad que se desea consultar.
     * @return datos resumidos de la unidad encontrada.
     * @throws IllegalArgumentException cuando no existe una unidad con el identificador indicado.
     */
    @Transactional(readOnly = true)
    public AbdDtos.UnitResponse findById(Integer idUnidad) {
        return unidadRepository.findById(idUnidad).map(this::aResponse)
                .orElseThrow(() -> new IllegalArgumentException("Unit no encontrada: " + idUnidad));
    }

    /**
     * Registra una unidad nueva verificando que la placa y el disco no esten duplicados.
     * @param request datos de la unidad que se desea registrar.
     * @return datos resumidos de la unidad guardada.
     * @throws IllegalArgumentException cuando la placa o el numero de disco ya estan registrados.
     */
    public AbdDtos.UnitResponse create(AbdDtos.UnitRequest request) {
        validateUniqueness(request.placa(), request.numeroDisco(), null);
        Unit unidad = Unit.builder()
                .placa(request.placa())
                .numeroDisco(request.numeroDisco())
                .modelo(request.modelo())
                .capacidad(request.capacidad())
                .anioFabricacion(request.anioFabricacion())
                .estado(request.estado() == null ? "Activo" : request.estado())
                .build();
        return aResponse(unidadRepository.save(unidad));
    }

    /**
     * Modifica los datos de una unidad existente verificando que no se dupliquen placa ni disco.
     * @param idUnidad identificador de la unidad que se desea modificar.
     * @param request nuevos datos que reemplazaran a los actuales de la unidad.
     * @return datos resumidos de la unidad actualizada.
     * @throws IllegalArgumentException cuando la unidad no existe o la placa o el disco ya pertenecen a otra unidad.
     */
    public AbdDtos.UnitResponse update(Integer idUnidad, AbdDtos.UnitRequest request) {
        Unit unidad = unidadRepository.findById(idUnidad)
                .orElseThrow(() -> new IllegalArgumentException("Unit no encontrada: " + idUnidad));
        validateUniqueness(request.placa(), request.numeroDisco(), idUnidad);
        unidad.setPlaca(request.placa());
        unidad.setNumeroDisco(request.numeroDisco());
        unidad.setModelo(request.modelo());
        unidad.setCapacidad(request.capacidad());
        unidad.setAnioFabricacion(request.anioFabricacion());
        if (request.estado() != null) {
            unidad.setEstado(request.estado());
        }
        return aResponse(unidadRepository.save(unidad));
    }

    /**
     * Suprime del sistema el registro de una unidad existente.
     * @param idUnidad identificador de la unidad que se desea suprimir.
     * @throws IllegalArgumentException cuando no existe una unidad con el identificador indicado.
     */
    public void delete(Integer idUnidad) {
        if (!unidadRepository.existsById(idUnidad)) {
            throw new IllegalArgumentException("Unit no encontrada: " + idUnidad);
        }
        unidadRepository.deleteById(idUnidad);
    }

    private void validateUniqueness(String placa, String numeroDisco, Integer idExcluir) {
        if (idExcluir == null) {
            if (unidadRepository.existsByLicensePlateIgnoreCase(placa)) {
                throw new IllegalArgumentException("Ya existe una unidad con la placa " + placa);
            }
            if (unidadRepository.existsByNumeroDiscoIgnoreCase(numeroDisco)) {
                throw new IllegalArgumentException("Ya existe una unidad con el disco " + numeroDisco);
            }
            return;
        }
        Unit actual = unidadRepository.findById(idExcluir)
                .orElseThrow(() -> new IllegalArgumentException("Unit no encontrada: " + idExcluir));
        boolean cambioPlaca = !actual.getPlaca().equalsIgnoreCase(placa);
        boolean cambioDisco = !actual.getNumeroDisco().equalsIgnoreCase(numeroDisco);
        if (cambioPlaca && unidadRepository.existsByLicensePlateIgnoreCase(placa)) {
            throw new IllegalArgumentException("Ya existe una unidad con la placa " + placa);
        }
        if (cambioDisco && unidadRepository.existsByNumeroDiscoIgnoreCase(numeroDisco)) {
            throw new IllegalArgumentException("Ya existe una unidad con el disco " + numeroDisco);
        }
    }

    private AbdDtos.UnitResponse aResponse(Unit u) {
        return new AbdDtos.UnitResponse(u.getIdUnidad(), u.getPlaca(), u.getNumeroDisco(),
                u.getModelo(), u.getCapacidad(), u.getAnioFabricacion(), u.getEstado());
    }
}
