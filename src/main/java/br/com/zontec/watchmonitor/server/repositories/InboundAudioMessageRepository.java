package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.InboundAudioMessage;

@Repository
public interface InboundAudioMessageRepository extends JpaRepository<InboundAudioMessage, Long> {

    List<InboundAudioMessage> findByDevice(Device device);

    List<InboundAudioMessage> findByDeviceAndTimestampAfter(Device device, LocalDateTime since);

    List<InboundAudioMessage> findByDeviceOrderByTimestampDesc(Device device, Pageable pageable);
}
