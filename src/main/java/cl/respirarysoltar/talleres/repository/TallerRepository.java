package cl.respirarysoltar.talleres.repository;

import cl.respirarysoltar.talleres.domain.Taller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TallerRepository extends JpaRepository<Taller, UUID> {

    List<Taller> findByFechaInicioAfterOrderByFechaInicioAsc(LocalDateTime desde);
}
