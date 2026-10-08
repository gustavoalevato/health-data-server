package br.com.zontec.watchmonitor.server.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "fall_down_alerts")
public class FallDownAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "latitude", precision = 10, scale = 8, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8, nullable = false)
    private BigDecimal longitude;

    @Column(name = "speed", precision = 5, scale = 2)
    private BigDecimal speed;

    @Column(name = "direction")
    private String direction;

    @Column(name = "battery_percentage")
    private Integer batteryPercentage;

    @Column(name = "gsm_strength")
    private Integer gsmStrength;

    @Column(name = "status_bits", length = 32, nullable = false)
    private String statusBits;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
