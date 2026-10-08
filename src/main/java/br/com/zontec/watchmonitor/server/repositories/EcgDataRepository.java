package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.EcgData;

@Repository
public interface EcgDataRepository extends JpaRepository<EcgData, Long> {

    // Buscar dados de ECG por dispositivo
    List<EcgData> findByDevice(Device device);

    // Buscar dados recentes
    @Query("SELECT e FROM EcgData e WHERE e.device = :device AND e.timestamp > :since")
    List<EcgData> findRecentEcgData(@Param("device") Device device, @Param("since") LocalDateTime since);

    // Buscar por pacote (para reconstruir ECG multipacote)
    List<EcgData> findByDeviceAndTotalPacketsAndCurrentPacket(Device device, Integer totalPackets, Integer currentPacket);
}