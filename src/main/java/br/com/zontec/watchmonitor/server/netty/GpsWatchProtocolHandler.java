package br.com.zontec.watchmonitor.server.netty;

import java.time.LocalDateTime;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.zontec.watchmonitor.server.command.BloodPressureCommand;
import br.com.zontec.watchmonitor.server.command.BodyTemperatureCommand;
import br.com.zontec.watchmonitor.server.command.ComprehensiveHealthCommand;
import br.com.zontec.watchmonitor.server.command.ConfigCommand;
import br.com.zontec.watchmonitor.server.command.DeviceCommand;
import br.com.zontec.watchmonitor.server.command.EcgDataCommand;
import br.com.zontec.watchmonitor.server.command.AlertCommand;
import br.com.zontec.watchmonitor.server.command.AudioMessageCommand;
import br.com.zontec.watchmonitor.server.command.HeartRateCommand;
import br.com.zontec.watchmonitor.server.command.HeartbeatCommand;
import br.com.zontec.watchmonitor.server.command.LinkKeepCommand;
import br.com.zontec.watchmonitor.server.command.LocationCommand;
import br.com.zontec.watchmonitor.server.command.LoginCommand;
import br.com.zontec.watchmonitor.server.command.SleepDataCommand;
import br.com.zontec.watchmonitor.server.command.Spo2Command;
import br.com.zontec.watchmonitor.server.command.UnknownCommand;
import br.com.zontec.watchmonitor.server.command.WeatherRequestCommand;
import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;
import br.com.zontec.watchmonitor.server.enums.ParserModel;
import br.com.zontec.watchmonitor.server.netty.response.DeviceProtocolHandler;
import br.com.zontec.watchmonitor.server.netty.response.DeviceProtocolHandlerFactory;
import br.com.zontec.watchmonitor.server.protocol.CommandParser;
import br.com.zontec.watchmonitor.server.protocol.CommandParserFactory;
import br.com.zontec.watchmonitor.server.protocol.Touch4PMessageEnvelope;
import br.com.zontec.watchmonitor.server.service.DeviceService;
import br.com.zontec.watchmonitor.server.utils.HexadecimalUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

public class GpsWatchProtocolHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(GpsWatchProtocolHandler.class);
    public static final AttributeKey<String> IMEI_ATTRIBUTE_KEY = AttributeKey.newInstance("IMEI");
    public static final AttributeKey<String> PARSER_MODEL_ATTRIBUTE_KEY = AttributeKey.newInstance("PARSER_MODEL");
    
    private final DeviceService deviceService;
    private final CommandParserFactory commandParserFactory;
    private final DeviceProtocolHandlerFactory deviceProtocolHandlerFactory;

    public GpsWatchProtocolHandler(DeviceService deviceService, CommandParserFactory commandParserFactory,
            DeviceProtocolHandlerFactory deviceProtocolHandlerFactory) {
        this.deviceService = deviceService;
        this.commandParserFactory = commandParserFactory;
        this.deviceProtocolHandlerFactory = deviceProtocolHandlerFactory;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {

        if (HexadecimalUtils.isHex(Unpooled.copiedBuffer(msg.getBytes()))) {
            log.warn("Mensagem recebida do dispositivo [{}] parece ser hexadecimal, mas foi tratada como string: '{}'. Verifique o parser ou o formato da mensagem.", ctx.channel().remoteAddress(), msg);
            msg = HexadecimalUtils.hexToString(msg);
        }
        
        log.info("Recebido do dispositivo [{}]: {}", ctx.channel().remoteAddress(), msg);

        String parserModel = ctx.channel().attr(PARSER_MODEL_ATTRIBUTE_KEY).get();
        if (parserModel == null || parserModel.isBlank()) {
            parserModel = resolveParserModel(ctx, msg);
            ctx.channel().attr(PARSER_MODEL_ATTRIBUTE_KEY).set(parserModel);
        }
        CommandParser parser = commandParserFactory.getParser(parserModel);
        DeviceProtocolHandler deviceProtocolHandler = deviceProtocolHandlerFactory.getHandler(parserModel);
        DeviceCommand command = parser.parse(msg);
        String imei = command.getImei();
        if (imei != null && !imei.isBlank()) {
            imei = deviceService.resolveImei(imei, parserModel);
        }

        // --- AUTENTICAÇÃO E PROCESSAMENTO DE LOGIN ---
        if (command.getType() == GPSWatchCommandType.LOGIN) {
            LoginCommand loginCommand = (LoginCommand) command;
            imei = deviceService.resolveImei(loginCommand.imei(), parserModel);

            if (deviceService.authenticateDevice(imei, parserModel)) {
                ctx.channel().attr(IMEI_ATTRIBUTE_KEY).set(imei);
                String parserModelForDevice = deviceService.getParserModelByImei(imei);
                ctx.channel().attr(PARSER_MODEL_ATTRIBUTE_KEY).set(parserModelForDevice);
                deviceProtocolHandler = deviceProtocolHandlerFactory.getHandler(parserModelForDevice);
                deviceService.registerAuthenticatedChannel(imei, ctx.channel().id());
                deviceService.registerChannel(imei, ctx.channel());
                deviceService.processDeviceLogin(imei);
                log.info("Parser '{}' selecionado para IMEI {}", parserModelForDevice, imei);

                int timezone = -3; 
                String response = deviceProtocolHandler.loginResponse(LocalDateTime.now(), timezone);
             
                ctx.writeAndFlush(response);
                log.info("Enviado para dispositivo [{}]: {}", ctx.channel().remoteAddress(), response);
            } else {
                log.warn("Login não autorizado tendado por IMEI {}. Closing connection.", imei);
                ctx.close();
            }
        }else if(command.getType() == GPSWatchCommandType.LINK_KEEP) {

            if (deviceService.authenticateDevice(imei, parserModel)) {
                ctx.channel().attr(IMEI_ATTRIBUTE_KEY).set(imei);
                String parserModelForDevice = deviceService.getParserModelByImei(imei);
                ctx.channel().attr(PARSER_MODEL_ATTRIBUTE_KEY).set(parserModelForDevice);
                deviceProtocolHandler = deviceProtocolHandlerFactory.getHandler(parserModelForDevice);
                deviceService.registerAuthenticatedChannel(imei, ctx.channel().id());
                deviceService.registerChannel(imei, ctx.channel());
                deviceService.processDeviceLogin(imei);
                log.info("Parser '{}' selecionado para IMEI {}", parserModelForDevice, imei);

                LinkKeepCommand lkCommand = (LinkKeepCommand) command;
                log.info("Processando Link Keep for IMEI {}: Bateria {}%, Passos {}", imei, lkCommand.batteryPercentage(), lkCommand.steps());
                deviceService.saveLinkKeep(imei, lkCommand);
                String response = buildTouch4PLinkKeepResponse(msg, parserModel, deviceProtocolHandler);
                if (!response.isBlank()) {
                    ctx.writeAndFlush(response);
                    log.info("Enviado para dispositivo [{}]: {}", ctx.channel().remoteAddress(), response);
                }
            }

            
        }
        // --- FIM DA AUTENTICAÇÃO E PROCESSAMENTO DE LOGIN ---
        else {
            // Para outros comandos, verificar se o canal já está autenticado
            String authenticatedImei = ctx.channel().attr(IMEI_ATTRIBUTE_KEY).get();
            if (authenticatedImei == null) {
                log.warn("Comando recebido de um dispositivo nao autenticado {}. Encerrando a conexao.", ctx.channel().remoteAddress());
                ctx.close();
                return;
            }

            // O IMEI do comando deve ser igual ao IMEI autenticado do canal, por segurança
            // Isso previne que um dispositivo autenticado envie comandos com IMEI de outro.
            if (imei != null && !imei.equals(authenticatedImei)) {
                log.warn("Comando recebido do tipo {} com IMEI incompativel '{}' a partir do canal autenticado como '{}' (expected '{}'). Encerrando conexao.",
                         command.getType(), imei, authenticatedImei, ctx.channel().remoteAddress());
                ctx.close();
                return;
            } else if (imei == null && authenticatedImei != null) {
                // assume-se o IMEI do canal para processamento
                imei = authenticatedImei;
            }
            
            log.info("Command received {}", command.getRawMessage());
            
            Random rand = new Random();

            switch (command.getType()) {
                case LOCATION:
                    LocationCommand locCommand = (LocationCommand) command;
                    log.info("Processando Location for IMEI {}: Lat {}, Lon {}", imei, locCommand.latitude(), locCommand.longitude());
                    deviceService.saveLocationData(imei, locCommand); // Ou passe os campos estruturados
                    writeProtocolAck(ctx, deviceProtocolHandler, command);
                    break;
                case HEARTBEAT:
                    HeartbeatCommand hbCommand = (HeartbeatCommand) command;
                    log.info("Processando Heartbeat for IMEI {}: Bateria {}%, Passos {}", imei, hbCommand.batteryPercentage(), hbCommand.steps());
                    deviceService.saveHeartbeat(imei, hbCommand);
                    writeProtocolAck(ctx, deviceProtocolHandler, command);
                    break;
                case HEART_RATE:
                    HeartRateCommand hrCommand = (HeartRateCommand) command;
                    log.info("Processando Heart Rate for IMEI {}: {} bpm", imei, hrCommand.heartRate());
                    deviceService.saveHeartRate(imei, hrCommand);
                    writeProtocolAck(ctx, deviceProtocolHandler, command);
                    break;
                case SPO2:
                    Spo2Command spo2Command = (Spo2Command) command;
                    log.info("Processando Spo2 for IMEI {}: {}", imei, spo2Command.spo2());
                    deviceService.saveOxygen(imei, spo2Command);
                    writeProtocolAck(ctx, deviceProtocolHandler, command);
                    break;
                case BLOOD_PRESSURE:
                    BloodPressureCommand bpCommand = (BloodPressureCommand) command;
                    log.info("Processando Blood Pressure for IMEI {}: {} bpm, {} pressao sistolica, {} pressão diastólica", imei, bpCommand.heartRate(), bpCommand.systolicPressure(), bpCommand.diastolicPressure());
                    deviceService.saveBloodPressure(imei, bpCommand);
                    writeProtocolAck(ctx, deviceProtocolHandler, command);
                    break;
                case COMPREHENSIVE_HEALTH: 
                    ComprehensiveHealthCommand chCommand = (ComprehensiveHealthCommand) command;
                    log.info("Processando Comprehensive Health for IMEI {}: HR={}, SBP={}, DBP={}, SpO2={}, BloodSugar={}, BodyTemp={}",
                             imei, chCommand.heartRate(), chCommand.systolicBloodPressure(),
                             chCommand.diastolicBloodPressure(), chCommand.spo2(), chCommand.bloodSugar(),
                             chCommand.bodyTemperature());
                    deviceService.saveComprehensiveHealthData(imei, chCommand); 
                    writeProtocolAck(ctx, deviceProtocolHandler, command);
                    break;
                case BODY_TEMPERATURE: // AP50
                    BodyTemperatureCommand btCommand = (BodyTemperatureCommand) command;
                    log.info("Processando Body Temperature for IMEI {}: {}°C", imei, btCommand.temperature());
                    deviceService.saveBodyTemperature(imei, btCommand);
                    writeProtocolAck(ctx, deviceProtocolHandler, command);
                    break;

                case SLEEP_DATA: // AP97
                    SleepDataCommand sdCommand = (SleepDataCommand) command;
                    log.info("Processando Sleep Data for IMEI {}: Date={}, Start={}, End={}, Total={}min",
                             imei, sdCommand.date(), sdCommand.startTime(), sdCommand.endTime(), sdCommand.totalSleepDurationMinutes());
                    deviceService.saveSleepData(imei, sdCommand);
                    writeProtocolAck(ctx, deviceProtocolHandler, command);
                    break;
                case ALERT:
                    AlertCommand alertCommand = (AlertCommand) command;
                    log.info("Processando Alert for IMEI {}: statusBits={}, Lat {}, Lon {}",
                            imei, alertCommand.statusBits(), alertCommand.latitude(), alertCommand.longitude());
                    deviceService.processAlert(imei, alertCommand);
                    writeProtocolAck(ctx, deviceProtocolHandler, command);
                    break;
                case CONFIG:
                    ConfigCommand configCommand = (ConfigCommand) command;
                    log.info("Processando Config for IMEI {}: {}", imei, configCommand.payload());
                    writeProtocolAck(ctx, deviceProtocolHandler, command);
                    break;

                case WEATHER_REQUEST: // APWT
                    WeatherRequestCommand wrCommand = (WeatherRequestCommand) command;
                    log.info("Processando Weather Request from IMEI {}: ", imei);
                    deviceService.processWeatherRequest(imei, wrCommand);
                    String weatherResponse = deviceProtocolHandler.weatherResponse(LocalDateTime.now(), rand);
                    if (!weatherResponse.isBlank()) {
                        ctx.writeAndFlush(weatherResponse);
                        log.info("Enviando resposta de clima para o dispositivo [{}]: {}", ctx.channel().remoteAddress(), weatherResponse);
                    }
                    break;

                case ECG_DATA: // APHD
                    EcgDataCommand edCommand = (EcgDataCommand) command;
                    log.info("Processando ECG Data for IMEI {}: Timestamp={}, TotalPkts={}, CurrentPkt={}, DataPoints={}",
                             imei, edCommand.timestamp(), edCommand.totalPackets(), edCommand.currentPacket(), edCommand.ecgDataPoints().size());
                    deviceService.saveEcgData(imei, edCommand);
                    String ecgResponse = deviceProtocolHandler.ecgAnalysisResponse(imei, rand);
                    if (!ecgResponse.isBlank()) {
                        ctx.writeAndFlush(ecgResponse);
                        log.info("Enviando analise do ECG para o dispositivo [{}]: {}", ctx.channel().remoteAddress(), ecgResponse);
                    }
                    break;
                case AUDIO_MESSAGE:
                    AudioMessageCommand audioMessageCommand = (AudioMessageCommand) command;
                    log.info("Processando Audio Message TK for IMEI {}: {} bytes", imei, audioMessageCommand.audioData().length);
                    deviceService.saveInboundAudioMessage(imei, audioMessageCommand);
                    writeProtocolAck(ctx, deviceProtocolHandler, command);
                    break;
                case UNKNOWN:
                    UnknownCommand uc = (UnknownCommand) command;
                    log.warn("Comando nao conhecido ou mal formatado a partir do IMEI {}: '{}' - Error: {}", authenticatedImei, uc.rawMessage(), uc.errorMessage());
                    // Pode fechar a conexão ou ignorar, dependendo da robustez desejada
                    break;
                default:
                    // Comandos reconhecidos mas não processados (TODO)
                    log.warn("Comando '{}' do IMEI {} reconhecido, mas nao processado. Raw: {}", command.getType(), imei, command.getRawMessage());
                    break;
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Dispositivo conectado: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String imei = ctx.channel().attr(IMEI_ATTRIBUTE_KEY).get();
        if (imei != null) {
            deviceService.unregisterAuthenticatedChannel(ctx.channel().id());
            deviceService.unregisterChannel(imei);
            log.info("Dispositivo autenticado {} desconectado: {}.", imei, ctx.channel().remoteAddress());
        } else {
            log.info("Dispositivo nao autenticado {} desconectado: {}.", ctx.channel().remoteAddress());
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String imei = ctx.channel().attr(IMEI_ATTRIBUTE_KEY).get();
        String identifier = imei != null ? "IMEI: " + imei : "Channel: " + ctx.channel().remoteAddress();
        log.error("Erro em {}: {}", identifier, cause.getMessage(), cause);
        ctx.close();
    }

    private String resolveParserModel(ChannelHandlerContext ctx, String msg) {
        String modelByPrefix = detectParserModelByPrefix(msg);
        if (modelByPrefix != null) {
            return modelByPrefix;
        }

        String imei = ctx.channel().attr(IMEI_ATTRIBUTE_KEY).get();
        if (imei == null || imei.isBlank()) {
            imei = extractImeiFromMessage(msg);
        }
        if (imei != null && !imei.isBlank()) {
            return deviceService.getParserModelByImei(imei);
        }

        return ParserModel.GPS_WATCH.name();
    }

    private void writeProtocolAck(ChannelHandlerContext ctx, DeviceProtocolHandler deviceProtocolHandler, DeviceCommand command) {
        String response = deviceProtocolHandler.acknowledge(command);
        if (response != null && !response.isBlank()) {
            ctx.writeAndFlush(response);
        }
    }

    private String buildTouch4PLinkKeepResponse(String rawMessage, String parserModel,
            DeviceProtocolHandler deviceProtocolHandler) {
        if (!ParserModel.TOUCH_4P.name().equals(parserModel)) {
            return deviceProtocolHandler.loginResponse(LocalDateTime.now(), -3);
        }

        Touch4PMessageEnvelope envelope = Touch4PMessageEnvelope.parse(rawMessage);
        return deviceProtocolHandler.loginResponse(envelope.manufacturer(), envelope.deviceId());
    }

    private String detectParserModelByPrefix(String msg) {
        if (msg == null || msg.isEmpty()) {
            return null;
        }

        int start = 0;
        while (start < msg.length() && msg.charAt(start) <= ' ') {
            start++;
        }
        if (start >= msg.length()) {
            return null;
        }

        if (msg.startsWith("IW", start)) {
            return ParserModel.GPS_WATCH.name();
        }

        char firstChar = msg.charAt(start);
        if (firstChar == '[' || firstChar == '*') {
            return ParserModel.TOUCH_4P.name();
        }

        return null;
    }

    private String extractImeiFromMessage(String msg) {
        if (msg == null) {
            return null;
        }

        int start = 0;
        while (start < msg.length() && msg.charAt(start) <= ' ') {
            start++;
        }
        if (!msg.startsWith("IWAP00", start)) {
            return null;
        }

        int imeiStart = start + 6;
        int imeiEnd = imeiStart;
        while (imeiEnd < msg.length() && Character.isDigit(msg.charAt(imeiEnd))) {
            imeiEnd++;
        }

        int len = imeiEnd - imeiStart;
        if (len < 14 || len > 17) {
            return null;
        }

        return msg.substring(imeiStart, imeiEnd);
    }
}
