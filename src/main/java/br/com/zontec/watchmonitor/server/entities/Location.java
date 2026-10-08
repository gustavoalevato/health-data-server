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
@Table(name = "locations")
public class Location {
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
    private BigDecimal speed;  // Velocidade em km/h

    @Column(name = "direction")
    private String direction;  // Direção em graus

    @Column(name = "altitude", precision = 7, scale = 2)
    private BigDecimal altitude;  // Altitude em metros (opcional, não usado no service atual)

    @Column(name = "battery_percentage")
    private Integer batteryPercentage;

    @Column(name = "gsm_strength")
    private Integer gsmStrength;

    @Column(name = "num_satellites")
    private Integer numSatellites;

    @Column(name = "steps")
    private Integer steps;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "wifi_ssid", length = 100)
    private String wifiSsid;

    @Column(name = "wifi_mac", length = 17)
    private String wifiMac;

    @Column(name = "wifi_rssi")
    private Integer wifiRssi;

    @Column(name = "is_sos_alert")
    private Boolean isSosAlert;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
}