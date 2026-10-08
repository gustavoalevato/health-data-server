package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.BloodPressure;
import br.com.zontec.watchmonitor.server.entities.Device;

@Repository
public interface BloodPressureRepository extends JpaRepository<BloodPressure, Long> {

    // Buscar pressões arteriais por dispositivo
    List<BloodPressure> findByDevice(Device device);

    // Buscar dados recentes
    @Query("SELECT bp FROM BloodPressure bp WHERE bp.device = :device AND bp.timestamp > :since ORDER BY bp.timestamp DESC")
    List<BloodPressure> findRecentBloodPressures(@Param("device") Device device, @Param("since") LocalDateTime since);

    // Média de pressão sistólica por dispositivo em um período
    @Query("SELECT AVG(bp.systolic) FROM BloodPressure bp WHERE bp.device = :device AND bp.timestamp BETWEEN :start AND :end")
    Double getAverageSystolic(@Param("device") Device device, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Média de pressão diastólica por dispositivo em um período
    @Query("SELECT AVG(bp.diastolic) FROM BloodPressure bp WHERE bp.device = :device AND bp.timestamp BETWEEN :start AND :end")
    Double getAverageDiastolic(@Param("device") Device device, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}