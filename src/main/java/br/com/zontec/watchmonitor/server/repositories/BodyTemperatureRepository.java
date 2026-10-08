package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.BodyTemperature;
import br.com.zontec.watchmonitor.server.entities.Device;

@Repository
public interface BodyTemperatureRepository extends JpaRepository<BodyTemperature, Long> {

    // Buscar temperaturas por dispositivo
    List<BodyTemperature> findByDevice(Device device);

    // Buscar dados recentes
    @Query("SELECT bt FROM BodyTemperature bt WHERE bt.device = :device AND bt.timestamp > :since")
    List<BodyTemperature> findRecentTemperatures(@Param("device") Device device, @Param("since") LocalDateTime since);

    // Média de temperatura por período
    @Query("SELECT AVG(bt.temperature) FROM BodyTemperature bt WHERE bt.device = :device AND bt.timestamp BETWEEN :start AND :end")
    Double getAverageTemperature(@Param("device") Device device, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}