package br.com.zontec.watchmonitor.server.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.enums.DeviceStatus;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    // Buscar dispositivo por IMEI (único)
    Optional<Device> findByImei(String imei);

    Optional<Device> findFirstByImeiEndingWithAndParserModel(String imeiSuffix, String parserModel);

    // Listar dispositivos ativos
    List<Device> findByStatus(DeviceStatus status);

    // Contar dispositivos online (última atividade nos últimos X minutos)
    @Query("SELECT COUNT(d) FROM Device d WHERE d.lastSeen > :threshold")
    long countOnlineDevices(@Param("threshold") java.time.LocalDateTime threshold);
}
