package ec.edu.uteq.sgroas.repository;

import ec.edu.uteq.sgroas.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    /**
     * Consulta el codigo de verificacion mas reciente de un correo y tipo dados.
     * @param email correo electronico asociado al codigo.
     * @param type tipo de codigo de verificacion.
     * @return codigo de verificacion mas reciente si existe.
     */
    Optional<VerificationCode> findFirstByEmailAndTypeOrderByCreatedAtDesc(String email, String type);

    /**
     * Elimina todos los codigos de verificacion de un correo y tipo dados.
     * @param email correo electronico asociado a los codigos a eliminar.
     * @param type tipo de codigo de verificacion a eliminar.
     */
    void deleteByEmailAndType(String email, String type);
}
