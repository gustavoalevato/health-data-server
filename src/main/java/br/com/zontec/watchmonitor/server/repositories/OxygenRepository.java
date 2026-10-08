package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.HeartRate;
import br.com.zontec.watchmonitor.server.entities.Oxygen;

@Repository
public interface OxygenRepository extends JpaRepository<Oxygen, Long> {

    // Buscar frequências cardíacas por dispositivo
    List<Oxygen> findByDevice(Device device);

    // Buscar dados recentes
    @Query("SELECT o FROM Oxygen o WHERE o.device = :device AND o.timestamp > :since")
    List<Oxygen> findRecentOxygenLevels(@Param("device") Device device, @Param("since") LocalDateTime since);

    // Média de HR por dia
    @Query("SELECT AVG(o.oxygenLevel), DATE(o.timestamp) FROM Oxygen o WHERE o.device = :device GROUP BY DATE(o.timestamp)")
    List<Object[]> getDailyAverageOxygenLevel(@Param("device") Device device);
}