package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.FallDownAlert;

@Repository
public interface FallDownAlertRepository extends JpaRepository<FallDownAlert, Long> {

    List<FallDownAlert> findByDevice(Device device);

    @Query("SELECT f FROM FallDownAlert f WHERE f.device = :device AND f.timestamp > :since")
    List<FallDownAlert> findRecentFallDownAlerts(@Param("device") Device device, @Param("since") LocalDateTime since);
}
