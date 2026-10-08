package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.SleepEntry;
import br.com.zontec.watchmonitor.server.entities.SleepSession;

@Repository
public interface SleepEntryRepository extends JpaRepository<SleepEntry, Long> {

    // Buscar entradas por sessão de sono
    List<SleepEntry> findBySleepSession(SleepSession sleepSession);

    @Query("SELECT e FROM SleepEntry e WHERE e.sleepSession.device = :device AND e.sleepSession.timestamp > :since")
    List<SleepEntry> findRecentSleepEntries(@Param("device") Device device, @Param("since") LocalDateTime since);
}
