package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.ComprehensiveHealth;
import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.SleepSession;

@Repository
public interface SleepSessionRepository extends JpaRepository<SleepSession, Long> {

    // Buscar sessões de sono por dispositivo
    List<SleepSession> findByDevice(Device device);

    // Buscar sessão por data específica
    List<SleepSession> findByDeviceAndSleepDate(Device device, LocalDate sleepDate);

    @Query("SELECT ch FROM SleepSession ch WHERE ch.device = :device AND ch.timestamp > :since")
    List<SleepSession> findRecentSleepSessions(@Param("device") Device device, @Param("since") LocalDateTime since);

    // Total de minutos de sono por dispositivo em um período
    @Query("SELECT SUM(ss.totalDurationMinutes) FROM SleepSession ss WHERE ss.device = :device AND ss.sleepDate BETWEEN :start AND :end")
    Integer getTotalSleepMinutes(@Param("device") Device device, @Param("start") LocalDate start, @Param("end") LocalDate end);
}