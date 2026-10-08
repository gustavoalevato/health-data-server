package br.com.zontec.watchmonitor.server.entities;

import java.time.LocalDateTime;

import br.com.zontec.watchmonitor.server.enums.ParserModel;
import br.com.zontec.watchmonitor.server.enums.DeviceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "devices")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 17)
    private String imei;
    
    private String description;

    @Column(nullable = false, length = 30)
    private String parserModel = ParserModel.GPS_WATCH.name();
    
    private Integer lastBatteryPercentage;

    @Enumerated(EnumType.STRING)
    private DeviceStatus status = DeviceStatus.ACTIVE;
    
    private LocalDateTime lastSeen;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    public Boolean isOnline() {
        if (lastSeen == null) {
            return false;
        }
        return lastSeen.isAfter(LocalDateTime.now().minusMinutes(20));
    }
    
}


