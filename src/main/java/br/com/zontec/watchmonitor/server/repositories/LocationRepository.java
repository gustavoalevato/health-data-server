package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    // Buscar localizações por dispositivo
    List<Location> findByDevice(Device device);

    // Buscar localizações recentes (últimas 24 horas)
    @Query("SELECT l FROM Location l WHERE l.device = :device AND l.timestamp > :since")
    List<Location> findRecentLocations(@Param("device") Device device, @Param("since") LocalDateTime since);

    // Buscar localizações por dispositivo em um período
    @Query("SELECT l FROM Location l WHERE l.device = :device AND l.isSosAlert = true AND l.timestamp > :since")
    List<Location> findLastSOSAlerts(@Param("device") Device device, @Param("since") LocalDateTime since);

    // Contar localizações por dispositivo em um período
    @Query("SELECT COUNT(l) FROM Location l WHERE l.device = :device AND l.timestamp BETWEEN :start AND :end")
    long countLocationsByDeviceAndPeriod(@Param("device") Device device, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
