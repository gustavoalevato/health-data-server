package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.HeartRate;

@Repository
public interface HeartRateRepository extends JpaRepository<HeartRate, Long> {

    // Buscar frequências cardíacas por dispositivo
    List<HeartRate> findByDevice(Device device);

    // Buscar dados recentes
    @Query("SELECT hr FROM HeartRate hr WHERE hr.device = :device AND hr.timestamp > :since")
    List<HeartRate> findRecentHeartRates(@Param("device") Device device, @Param("since") LocalDateTime since);

    // Média de HR por dia
    @Query("SELECT AVG(hr.heartRate), DATE(hr.timestamp) FROM HeartRate hr WHERE hr.device = :device GROUP BY DATE(hr.timestamp)")
    List<Object[]> getDailyAverageHeartRate(@Param("device") Device device);
}