package br.com.zontec.watchmonitor.server.protocol;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.zontec.watchmonitor.server.command.BloodPressureCommand;
import br.com.zontec.watchmonitor.server.command.BodyTemperatureCommand;
import br.com.zontec.watchmonitor.server.command.ComprehensiveHealthCommand;
import br.com.zontec.watchmonitor.server.command.DeviceCommand;
import br.com.zontec.watchmonitor.server.command.EcgDataCommand;
import br.com.zontec.watchmonitor.server.command.HeartRateCommand;
import br.com.zontec.watchmonitor.server.command.HeartbeatCommand;
import br.com.zontec.watchmonitor.server.command.LocationCommand;
import br.com.zontec.watchmonitor.server.command.LoginCommand;
import br.com.zontec.watchmonitor.server.command.SleepDataCommand;
import br.com.zontec.watchmonitor.server.command.SleepEntryCommand;
import br.com.zontec.watchmonitor.server.command.UnknownCommand;
import br.com.zontec.watchmonitor.server.command.WeatherRequestCommand;
import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;
import br.com.zontec.watchmonitor.server.enums.ParserModel;

@Component
public class GpsWatchCommandParser implements CommandParser {

    private static final Logger log = LoggerFactory.getLogger(GpsWatchCommandParser.class);
    private static final String HEADER_PREFIX = "IW"; 
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHMMSS");
    
    private static final DateTimeFormatter TIME_FORMATTER_AP97 = DateTimeFormatter.ofPattern("HHMM");

    @Override
    public String getParserModel() {
        return ParserModel.GPS_WATCH.name();
    }

    @Override
    public DeviceCommand parse(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return new UnknownCommand(null, rawMessage, "Mensagem vazia ou nula.");
        }

        // Remove o # final se o DelimiterBasedFrameDecoder não o fez (depende da configuração)
        String cleanedMessage = rawMessage.endsWith("#") ? rawMessage.substring(0, rawMessage.length() - 1) : rawMessage;
        
        // Removendo quebras de linha
        cleanedMessage = cleanedMessage.replaceAll("[\\r\\n]", "");

        // Verifica o prefixo padrão
        if (!cleanedMessage.startsWith(HEADER_PREFIX)) {
            return new UnknownCommand(null, rawMessage, "Mensagem não começa com o prefixo esperado 'IW'.");
        }

        // Extrai o código do comando (AP00, AP01, etc.)
        // Assume que o código do comando tem 4 caracteres após "IW" (ex: "IWAP00")
        if (cleanedMessage.length() < HEADER_PREFIX.length() + 4) {
             return new UnknownCommand(null, rawMessage, "Mensagem muito curta para conter o código do comando.");
        }
        String commandCode = cleanedMessage.substring(HEADER_PREFIX.length(), HEADER_PREFIX.length() + 4);
        String payload = cleanedMessage.substring(HEADER_PREFIX.length() + 4); // O resto da mensagem

        String imei = null; // Tenta extrair o IMEI para mensagens de erro, se possível
        
        int battery = 0;
        int gsm = 0;
        int heartRate = 0;

        try {
            switch (GPSWatchCommandType.fromCode(commandCode)) {
                case LOGIN:
                    // Formato: IWAP00IMEI
                    imei = payload; // Para AP00, o payload é o IMEI inteiro
                    if (imei.length() < 15 || imei.length() > 17) { // IMEI geralmente tem 15-17 dígitos
                        throw new IllegalArgumentException("IMEI inválido para AP00: " + imei);
                    }
                    return new LoginCommand(imei, rawMessage);

                case LOCATION:
                    
                    if (payload.length() < 50) { // Precisa de pelo menos IMEI até Battery
                        throw new IllegalArgumentException("Número insuficiente de campos para AP01.");
                    }
                    String date = payload.substring(0, 6);
                    String isDateValid = payload.substring(6, 7);
                    
                    String degreesNL = payload.substring(7, 9);
                    BigDecimal latitude = new BigDecimal(payload.substring(9, 16));
                    String direction1 = payload.substring(16, 17);
                    String degreesEL = payload.substring(17, 20);
                    BigDecimal longitude = new BigDecimal(payload.substring(20, 27));
                    String direction2 =  payload.substring(27, 28);
                    BigDecimal speed = new BigDecimal(payload.substring(28, 33));
                    String time = payload.substring(33, 39);
                    BigDecimal directionAngle = new BigDecimal(payload.substring(39, 45));

                    return new LocationCommand(imei, latitude, longitude, speed, direction1, battery, gsm, rawMessage);

                case HEARTBEAT:
                    String[] heartbeatParts = payload.split(",");
                    if (heartbeatParts.length < 3) { // Pelo menos IMEI, Battery, GSM, NumSats, Status, Steps
                        throw new IllegalArgumentException("Número insuficiente de campos para AP03.");
                    }
                    String heartBeatData = heartbeatParts[1];
                    
                    gsm = Integer.parseInt(heartBeatData.substring(0, 3));
                    int numSats = Integer.parseInt(heartBeatData.substring(3, 6));
                    battery = Integer.parseInt(heartBeatData.substring(6, 9));
                    int spaceLeft = Integer.parseInt(heartBeatData.substring(9, 10)); 
                    String status =  heartBeatData.substring(11, 12);
                    int steps = Integer.parseInt(heartbeatParts[2]);
                    int rollsFrequency = Integer.parseInt(heartbeatParts[3]);
                    
  
                    return new HeartbeatCommand(imei, battery, gsm, numSats, steps, spaceLeft, rollsFrequency, status, rawMessage);

                case HEART_RATE:
                    // Formato: IWAP49,HeartRate
                    String[] hrParts = payload.split(",");
                    if (hrParts.length < 2) {
                        throw new IllegalArgumentException("Número insuficiente de campos para AP49.");
                    }
                    
                    heartRate = Integer.parseInt(hrParts[1]);
                    return new HeartRateCommand(imei, heartRate, rawMessage);
                
                case BLOOD_PRESSURE:
                	// Formato: IWAPHT,HeartRate, SystolicPressure, DiastolicPressure
                    String[] bpParts = payload.split(",");
                    if (bpParts.length < 3) {
                        throw new IllegalArgumentException("Número insuficiente de campos para IWAPHT.");
                    }
                    
                    heartRate = Integer.parseInt(bpParts[1]);
                    int systolicPressure = Integer.parseInt(bpParts[2]);
                    int diastolicPressure = Integer.parseInt(bpParts[3]);
                    
                    return new BloodPressureCommand(imei, heartRate, systolicPressure, diastolicPressure, rawMessage);
                	
                case COMPREHENSIVE_HEALTH:
                    // Formato: IMEI,Heart rate,Systolic Blood Pressure,Diastolic Blood Pressure,SpO2,Blood sugar,Body temperature
                    String[] chParts = payload.split(",");
                    if (chParts.length < 7) { // Espera 7 campos após o IMEI
                        throw new IllegalArgumentException("Número insuficiente de campos para APHP (Saúde Abrangente).");
                    }
                    heartRate = Integer.parseInt(chParts[1]);
                    int systolicBp = Integer.parseInt(chParts[2]);
                    int diastolicBp = Integer.parseInt(chParts[3]);
                    int spo2 = Integer.parseInt(chParts[4]);
                    BigDecimal bloodSugar = new BigDecimal(chParts[5]);
                    BigDecimal bodyTemperature = new BigDecimal(chParts[6]);

                    return new ComprehensiveHealthCommand(imei, heartRate, systolicBp, diastolicBp, spo2, bloodSugar, bodyTemperature, rawMessage);
               
                case BODY_TEMPERATURE: // AP50
                    // Formato: IMEI,Temperature
                    String[] btParts = payload.split(",");
                    if (btParts.length < 2) {
                        throw new IllegalArgumentException("Número insuficiente de campos para AP50 (Temperatura Corporal).");
                    }
                    
                    BigDecimal temp = new BigDecimal(btParts[1]);
                    return new BodyTemperatureCommand(imei, temp, rawMessage);

                case SLEEP_DATA: // AP97
                    // Formato: Command,HHMM@HHMM,SleepDuration,StatusSleepDescription#
                    String[] sdParts = payload.split(",");
                    if (sdParts.length < 4) { // imei,date,start,end,type1,duration1,totalDuration
                        throw new IllegalArgumentException("Número insuficiente de campos para AP97 (Dados de Sono).");
                    }
                    
                    LocalDate data = LocalDate.now();
                    
                    String[] sleepPeriod = sdParts[1].split("@");
                    LocalTime startTime = LocalTime.parse(sleepPeriod[0], TIME_FORMATTER_AP97);
                    LocalTime endTime = LocalTime.parse(sleepPeriod[1], TIME_FORMATTER_AP97);
                    
                    int totalSleepDuration = Integer.parseInt(sdParts[2]);

                    List<SleepEntryCommand> sleepEntries = new ArrayList<>();
                    // As entradas de sono começam no índice 4 e vêm em pares (tipo, duração)
                    for (int i = 4; i < sdParts.length - 1; i += 2) {
                        int sleepType = Integer.parseInt(sdParts[i]);
                        int duration = Integer.parseInt(sdParts[i+1]);
                        sleepEntries.add(new SleepEntryCommand(imei, sleepType, duration, rawMessage));
                    }

                    return new SleepDataCommand(imei, data, startTime, endTime, sleepEntries, totalSleepDuration, rawMessage);

                case WEATHER_REQUEST: // APWT
                    String[] wrParts = payload.split("#");
                    if (wrParts.length < 1) {
                        throw new IllegalArgumentException("Número insuficiente de campos para APWT (Solicitação de Clima).");
                    }
                    
                    String city = "";
                    if (wrParts.length > 1) {
                    	city = wrParts[1];
                    }
                    
                    return new WeatherRequestCommand(imei, rawMessage, city);

                case ECG_DATA: // APHD
                    // Formato: IMEI,YYYYMMDDHHMMSS,Gain,ZeroVoltage,LeadType,SamplingRate,TotalPkt,CurrentPkt,ECGData(int,int,...)...
                    String[] edParts = payload.split(",");
                    if (edParts.length < 9) { // IMEI até ECGData
                        throw new IllegalArgumentException("Número insuficiente de campos para APHD (Dados de ECG).");
                    }
                    
                    LocalDateTime timestamp = LocalDateTime.parse(edParts[1], DATE_TIME_FORMATTER);
                    int totalPackets = Integer.parseInt(edParts[2]);
                    int currentPacket = Integer.parseInt(edParts[3]);
                    BigDecimal gain = new BigDecimal(edParts[4]);
                    BigDecimal zeroVoltage = new BigDecimal(edParts[5]);
                    int leadType = Integer.parseInt(edParts[6]);
                    int samplingRate = Integer.parseInt(edParts[7]);
                    
                    int maxDatalength = Integer.parseInt(edParts[8]);
                    
                    byte[] ecgDataPointsArr = edParts[9].getBytes();
                    ByteBuffer buffer = ByteBuffer.wrap(ecgDataPointsArr).order(ByteOrder.LITTLE_ENDIAN);
      
                    List<String> ecgDataPoints = new ArrayList<>();
                    for (int i = 0; i < ecgDataPointsArr.length; i++) {
                        ecgDataPoints.add(String.valueOf(ecgDataPointsArr[i] & 0xFF));
                    }
        
                    return new EcgDataCommand(imei, timestamp, totalPackets, currentPacket, gain, zeroVoltage, leadType, samplingRate, ecgDataPoints, rawMessage);
    
                default:
                    return new UnknownCommand(imei, rawMessage, "Comando '" + commandCode + "' reconhecido, mas não implementado.");
            }
        } catch (NumberFormatException e) {
            log.error("Erro de formato numérico ao parsear comando: {}. Mensagem: {}", e.getMessage(), rawMessage, e);
            return new UnknownCommand(imei, rawMessage, "Erro de formato numérico: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Erro de validação ao parsear comando: {}. Mensagem: {}", e.getMessage(), rawMessage, e);
            return new UnknownCommand(imei, rawMessage, "Erro de validação: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao parsear comando: {}. Mensagem: {}", e.getMessage(), rawMessage, e);
            return new UnknownCommand(imei, rawMessage, "Erro inesperado: " + e.getMessage());
        }
    }
}
