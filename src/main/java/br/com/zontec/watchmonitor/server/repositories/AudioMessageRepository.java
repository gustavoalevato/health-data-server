package br.com.zontec.watchmonitor.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.AudioMessage;
import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.InboundAudioMessage;

@Repository
public interface AudioMessageRepository extends JpaRepository<AudioMessage, Long> {

    List<AudioMessage> findByDevice(Device device);

    List<AudioMessage> findByDeviceAndTimestampAfter(Device device, LocalDateTime since);

    List<AudioMessage> findByDeviceOrderByTimestampDesc(Device device, Pageable pageable);
}
