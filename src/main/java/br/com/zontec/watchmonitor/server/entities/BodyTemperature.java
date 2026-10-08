package br.com.zontec.watchmonitor.server.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

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
@Table(name = "body_temperatures")
public class BodyTemperature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "temperature", precision = 4, scale = 2, nullable = false)
    private BigDecimal temperature;  // Temperatura em °C (ex: 36.50)

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public BodyTemperature(Device device, BigDecimal temperature, LocalDateTime timestamp) {
        this.device = device;
        this.temperature = temperature;
        this.timestamp = timestamp;
    }
}