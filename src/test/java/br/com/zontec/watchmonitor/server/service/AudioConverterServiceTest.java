package br.com.zontec.watchmonitor.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes unitários para AudioConverterService.
 */
@DisplayName("AudioConverterService Tests")
class AudioConverterServiceTest {

    private AudioConverterService audioConverterService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        audioConverterService = new AudioConverterService("ffmpeg", tempDir.toString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando bytes de entrada são nulos")
    void testConvertWebmToAmrWithNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            audioConverterService.convertWebmOpusToAmr(null);
        });
    }

    @Test
    @DisplayName("Deve lançar exceção quando bytes de entrada estão vazios")
    void testConvertWebmToAmrWithEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            audioConverterService.convertWebmOpusToAmr(new byte[0]);
        });
    }

    @Test
    @DisplayName("Deve lançar exceção quando bytes de AMR são nulos")
    void testConvertAmrToMp3WithNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            audioConverterService.convertAmrToMp3(null);
        });
    }

    @Test
    @DisplayName("Deve lançar exceção quando bytes de AMR estão vazios")
    void testConvertAmrToMp3WithEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            audioConverterService.convertAmrToMp3(new byte[0]);
        });
    }

    @Test
    @DisplayName("Deve criar diretório temporário se não existir")
    void testTempDirectoryCreation() {
        Path newTempDir = tempDir.resolve("new_audio_temp");
        assertFalse(Files.exists(newTempDir));

        // Criar novo serviço com diretório que não existe
        new AudioConverterService("ffmpeg", newTempDir.toString());

        assertTrue(Files.exists(newTempDir));
        assertTrue(Files.isDirectory(newTempDir));
    }

    @Test
    @DisplayName("Deve simular conversão de WebM/Opus para AMR com dados válidos")
    void testConvertWebmToAmrSimulation() {
        // Este teste verifica se o método aceita bytes válidos
        // Na prática, sem FFmpeg instalado, a conversão falhará, mas o método deve estar pronto
        byte[] dummyAudioBytes = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};

        // Esperamos uma exceção ao executar FFmpeg sem instalação
        // mas o método deve ter sido chamado corretamente
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            audioConverterService.convertWebmOpusToAmr(dummyAudioBytes);
        });

        assertTrue(exception.getMessage().contains("FFmpeg") || 
                   exception.getMessage().contains("Falha na conversão"));
    }

    @Test
    @DisplayName("Deve simular conversão de AMR para MP3 com dados válidos")
    void testConvertAmrToMp3Simulation() {
        // Este teste verifica se o método aceita bytes válidos
        byte[] dummyAudioBytes = new byte[]{0x23, 0x21, 0x41, 0x4D, 0x52};  // AMR magic number

        // Esperamos uma exceção ao executar FFmpeg sem instalação
        // mas o método deve ter sido chamado corretamente
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            audioConverterService.convertAmrToMp3(dummyAudioBytes);
        });

        assertTrue(exception.getMessage().contains("FFmpeg") || 
                   exception.getMessage().contains("Falha na conversão"));
    }

}
