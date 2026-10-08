package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.ComprehensiveHealth;
import br.com.zontec.watchmonitor.server.entities.Device;

@Repository
public interface ComprehensiveHealthRepository extends JpaRepository<ComprehensiveHealth, Long> {

    // Buscar dados de saúde por dispositivo
    List<ComprehensiveHealth> findByDevice(Device device);

    // Buscar dados recentes
    @Query("SELECT ch FROM ComprehensiveHealth ch WHERE ch.device = :device AND ch.timestamp > :since")
    List<ComprehensiveHealth> findRecentHealthData(@Param("device") Device device, @Param("since") LocalDateTime since);

    // Média de frequência cardíaca por dispositivo em um período
    @Query("SELECT AVG(ch.heartRate) FROM ComprehensiveHealth ch WHERE ch.device = :device AND ch.timestamp BETWEEN :start AND :end")
    Double getAverageHeartRate(@Param("device") Device device, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}