package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.Heartbeat;

@Repository
public interface HeartbeatRepository extends JpaRepository<Heartbeat, Long> {

    // Buscar heartbeats por dispositivo
    List<Heartbeat> findByDevice(Device device);

    // Buscar heartbeats recentes
    @Query("SELECT h FROM Heartbeat h WHERE h.device = :device AND h.timestamp > :since ORDER BY h.timestamp DESC")
    List<Heartbeat> findRecentHeartbeats(Device device, LocalDateTime since);

    // Média de bateria por dispositivo nos últimos dias
    @Query("SELECT AVG(h.batteryPercentage) FROM Heartbeat h WHERE h.device = :device AND h.timestamp > :since")
    Double getAverageBattery(Device device, LocalDateTime since);
}