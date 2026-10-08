package br.com.zontec.watchmonitor.server.messaging.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SOSAlertPayload {
    
    public String imei;
    public BigDecimal latitude;
    public BigDecimal longitude;

}
