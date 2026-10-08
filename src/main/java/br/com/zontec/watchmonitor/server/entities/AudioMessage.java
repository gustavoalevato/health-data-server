package br.com.zontec.watchmonitor.server.entities;

import java.time.LocalDateTime;

import br.com.zontec.watchmonitor.server.enums.AudioMessageDirections;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audio_messages")
public class AudioMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false, length = 1024)
    private String filePath;

    @Column(nullable = false, length = 20)
    private AudioMessageDirections direction; // "INBOUND" ou "OUTBOUND"
    
    private Boolean sended;

    private LocalDateTime timestamp;
}
