package br.com.zontec.watchmonitor.server.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Serviço para conversão de formatos de áudio.
 * Suporta conversão entre WebM/Opus, AMR e MP3 utilizando FFmpeg.
 */
@Service
public class AudioConverterService {

    private static final Logger log = LoggerFactory.getLogger(AudioConverterService.class);

    private final String ffmpegPath;
    private final Path tempDirectory;

    public AudioConverterService(
            @Value("${audio.ffmpeg.path:ffmpeg}") String ffmpegPath,
            @Value("${audio.temp.directory:./data/audio/temp}") String tempDirectory) {
        this.ffmpegPath = ffmpegPath;
        this.tempDirectory = Paths.get(tempDirectory).toAbsolutePath().normalize();
        initializeTempDirectory();
    }

    private void initializeTempDirectory() {
        try {
            Files.createDirectories(tempDirectory);
            log.info("Diretório temporário de áudio criado em: {}", tempDirectory);
        } catch (IOException e) {
            log.error("Erro ao criar diretório temporário de áudio", e);
            throw new RuntimeException("Não foi possível criar diretório temporário para conversão de áudio.", e);
        }
    }

    /**
     * Converte áudio de WebM/Opus para AMR.
     *
     * @param webmAudioBytes Bytes do arquivo de áudio WebM/Opus
     * @return Bytes do arquivo convertido para AMR
     * @throws IllegalArgumentException Se a conversão falhar
     */
    public byte[] convertWebmOpusToAmr(byte[] webmAudioBytes) {
        return convertAudio(webmAudioBytes, "webm_opus", ".webm", ".amr", getAmrConversionCommand());
    }

    /**
     * Converte áudio de AMR para MP3.
     *
     * @param amrAudioBytes Bytes do arquivo de áudio AMR
     * @return Bytes do arquivo convertido para MP3
     * @throws IllegalArgumentException Se a conversão falhar
     */
    public byte[] convertAmrToMp3(byte[] amrAudioBytes) {
        return convertAudio(amrAudioBytes, "amr", ".amr", ".mp3", getAmrToMp3ConversionCommand());
    }

    /**
     * Método genérico de conversão de áudio.
     *
     * @param inputBytes      Bytes do arquivo de entrada
     * @param processName     Nome do processo para logging
     * @param inputExtension  Extensão do arquivo de entrada
     * @param outputExtension Extensão do arquivo de saída
     * @param commandBuilder  Função para construir o comando FFmpeg
     * @return Bytes do arquivo convertido
     */
    private byte[] convertAudio(byte[] inputBytes, String processName, String inputExtension,
            String outputExtension, CommandBuilder commandBuilder) {
        if (inputBytes == null || inputBytes.length == 0) {
            throw new IllegalArgumentException("Arquivo de áudio de entrada não pode estar vazio.");
        }

        String inputFileName = processName + "_input_" + System.nanoTime() + inputExtension;
        String outputFileName = processName + "_output_" + System.nanoTime() + outputExtension;

        Path inputFile = tempDirectory.resolve(inputFileName);
        Path outputFile = tempDirectory.resolve(outputFileName);

        try {
            // Escrever arquivo de entrada
            Files.write(inputFile, inputBytes);
            log.debug("Arquivo de entrada criado: {}", inputFile);

            // Construir e executar comando FFmpeg
            String command = commandBuilder.build(inputFile.toString(), outputFile.toString());
            executeFFmpegCommand(command);

            // Verificar se arquivo de saída foi criado
            if (!Files.exists(outputFile)) {
                throw new IllegalArgumentException(
                        "Falha na conversão de áudio: arquivo de saída não foi criado.");
            }

            // Ler arquivo convertido
            byte[] convertedAudio = Files.readAllBytes(outputFile);
            log.debug("Áudio convertido com sucesso. Tamanho: {} bytes", convertedAudio.length);

            return convertedAudio;

        } catch (IOException e) {
            log.error("Erro ao processar conversão de áudio", e);
            throw new IllegalArgumentException("Falha ao converter áudio: " + e.getMessage(), e);
        } finally {
            // Limpar arquivos temporários
            deleteIfExists(inputFile);
            deleteIfExists(outputFile);
        }
    }

    /**
     * Executa o comando FFmpeg.
     *
     * @param command Comando a executar
     * @throws IllegalArgumentException Se a execução falhar
     */
    private void executeFFmpegCommand(String command) {
        try {
            log.debug("Executando comando FFmpeg: {}", command);
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            boolean completed = process.waitFor(60, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                throw new IllegalArgumentException("Conversão de áudio expirou (timeout).");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalArgumentException(
                        "FFmpeg retornou código de erro: " + exitCode);
            }

            log.debug("Comando FFmpeg executado com sucesso");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Conversão de áudio foi interrompida.", e);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Erro ao executar FFmpeg. Verifique se FFmpeg está instalado: " + e.getMessage(), e);
        }
    }

    /**
     * Constrói o comando para conversão de WebM/Opus para AMR.
     */
    private CommandBuilder getAmrConversionCommand() {
        return (input, output) -> String.format(
                "%s -i \"%s\" -acodec libopencore_amrnb -ac 1 -ar 8000 -ab 12.2k \"%s\" -y",
                ffmpegPath, input, output);
    }

    /**
     * Constrói o comando para conversão de AMR para MP3.
     */
    private CommandBuilder getAmrToMp3ConversionCommand() {
        return (input, output) -> String.format(
                "%s -i \"%s\" -acodec libmp3lame -q:a 4 \"%s\" -y",
                ffmpegPath, input, output);
    }

    /**
     * Deleta arquivo se existir.
     */
    private void deleteIfExists(Path file) {
        try {
            Files.deleteIfExists(file);
            log.debug("Arquivo temporário deletado: {}", file);
        } catch (IOException e) {
            log.warn("Não foi possível deletar arquivo temporário: {}", file, e);
        }
    }

    /**
     * Interface para construção de comandos FFmpeg.
     */
    @FunctionalInterface
    private interface CommandBuilder {
        String build(String inputPath, String outputPath);
    }
}
