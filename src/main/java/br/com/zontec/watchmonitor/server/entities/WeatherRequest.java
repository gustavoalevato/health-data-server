package br.com.zontec.watchmonitor.server.entities;

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
@Table(name = "weather_requests")
public class WeatherRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "city_code", length = 10, nullable = true)
    private String cityCode;  // Código da cidade (ex: "BRSAO")

    @Column(name = "city", length = 100, nullable = true)
    private String city;  // Nome da cidade (ex: "São Paulo")

    @Column(name = "language", length = 5, nullable = true)
    private String language;  // Idioma (ex: "pt", "en")

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

}