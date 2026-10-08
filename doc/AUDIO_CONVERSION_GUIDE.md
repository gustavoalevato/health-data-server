# Conversão de Áudio - Documentação

## Visão Geral

O serviço de conversão de áudio foi implementado para permitir:

1. **Conversão de WebM/Opus → AMR**: Áudios gravados no navegador (WebM/Opus) são convertidos para AMR antes de serem enviados ao dispositivo Touch4P
2. **Conversão de AMR → MP3**: Áudios recebidos do dispositivo (AMR) são convertidos para MP3 para reprodução no navegador

## Requisitos

### Dependências Java
- **FFmpeg 4j** (versão 0.8.0): Wrapper Java para FFmpeg
- Spring Boot 3.5.9
- Java 21

### Requisitos do Sistema
- **FFmpeg**: Deve estar instalado e acessível no PATH do sistema
  - Linux: `sudo apt-get install ffmpeg`
  - macOS: `brew install ffmpeg`
  - Windows: Download de https://ffmpeg.org/download.html

### Verificar Instalação do FFmpeg
```bash
ffmpeg -version
```

## Configuração

### application.properties

As seguintes propriedades foram adicionadas para configurar o serviço:

```properties
# Caminho para o executável FFmpeg (padrão: "ffmpeg")
audio.ffmpeg.path=ffmpeg

# Diretório temporário para arquivos durante conversão (padrão: "./data/audio/temp")
audio.temp.directory=./data/audio/temp
```

### Personalização

Se o FFmpeg estiver em um local não-padrão, configure:

```properties
# Linux/macOS
audio.ffmpeg.path=/usr/bin/ffmpeg

# Windows
audio.ffmpeg.path=C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe
```

## Estrutura da Implementação

### 1. AudioConverterService
**Localização**: `br.com.zontec.watchmonitor.server.service.AudioConverterService`

Responsável por:
- Conversão de WebM/Opus para AMR
- Conversão de AMR para MP3
- Gerenciamento de arquivos temporários
- Execução de comandos FFmpeg

**Métodos principais**:
```java
// Converte WebM/Opus para AMR
public byte[] convertWebmOpusToAmr(byte[] webmAudioBytes)

// Converte AMR para MP3
public byte[] convertAmrToMp3(byte[] amrAudioBytes)
```

### 2. DeviceAudioController - Método: sendAudioToDevice
**Localização**: `br.com.zontec.watchmonitor.server.api.controllers:DeviceAudioController:47-83`

**Fluxo**:
```
1. Recebe arquivo MultipartFile com MIME type "audio/webm;codecs=opus"
2. Extrai bytes do arquivo
3. Se for WebM/Opus, converte para AMR usando AudioConverterService
4. Envia dados AMR convertidos ao dispositivo via DeviceService.sendAudioMessage()
5. Retorna resultado
```

**Endpoint**:
```
POST /api/devices/{imei}/audios/send
Content-Type: multipart/form-data

Body:
  file: [arquivo WebM ou AMR]
```

### 3. DeviceAudioController - Método: streamAudio
**Localização**: `br.com.zontec.watchmonitor.server.api.controllers:DeviceAudioController:114-146`

**Fluxo**:
```
1. Recebe ID do áudio armazenado
2. Carrega arquivo AMR do disco
3. Converte AMR para MP3 usando AudioConverterService
4. Retorna stream MP3 com headers apropriados
```

**Endpoint**:
```
GET /api/device-audios/{audioId}/stream

Response:
  Content-Type: audio/mpeg
  Content-Disposition: inline; filename="audio.mp3"
  [arquivo MP3 em stream]
```

## Fluxos de Dados

### Fluxo de Envio (Navegador → Dispositivo)

```
┌─────────────────┐
│  Navegador      │
│  Gravação       │
│  WebM/Opus      │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  POST /devices/{imei}/audios/send   │
│  Content-Type: audio/webm;...opus   │
└────────┬────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│  DeviceAudioController.sendAudioToDevice │
└────────┬─────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│  AudioConverterService                   │
│  convertWebmOpusToAmr()                  │
│  WebM/Opus ──FFmpeg──> AMR               │
└────────┬─────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────┐
│  DeviceService.sendAudioMessage()      │
│  Envia AMR ao Touch4P via Netty        │
└────────┬───────────────────────────────┘
         │
         ▼
┌────────────────────┐
│  Smart Watch       │
│  Touch4P           │
│  Recebe AMR        │
└────────────────────┘
```

### Fluxo de Reprodução (Dispositivo → Navegador)

```
┌────────────────────┐
│  Smart Watch       │
│  Touch4P           │
│  Envia AMR         │
└────────┬───────────┘
         │
         ▼
┌──────────────────────────────────┐
│  DeviceService.saveAudioMessage()│
│  Salva em disco como .amr        │
└────────┬───────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│  GET /device-audios/{audioId}/stream     │
└────────┬─────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│  DeviceAudioController.streamAudio()     │
└────────┬─────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│  AudioConverterService                   │
│  convertAmrToMp3()                       │
│  AMR ──FFmpeg──> MP3                     │
└────────┬─────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│  ResponseEntity com Stream MP3           │
│  Content-Type: audio/mpeg                │
└────────┬─────────────────────────────────┘
         │
         ▼
┌─────────────────┐
│  Navegador      │
│  Reprodução     │
│  MP3            │
└─────────────────┘
```

## Tratamento de Erros

### Exceções Possíveis

| Exceção | Causa | Ação |
|---------|-------|------|
| `IllegalArgumentException: Arquivo de áudio de entrada não pode estar vazio.` | Bytes nulos ou vazios | Validar entrada |
| `IllegalArgumentException: FFmpeg retornou código de erro: X` | FFmpeg não conseguiu converter | Verificar arquivo de entrada |
| `IllegalArgumentException: Erro ao executar FFmpeg. Verifique se FFmpeg está instalado` | FFmpeg não encontrado | Instalar FFmpeg |
| `IllegalArgumentException: Conversão de áudio expirou (timeout).` | Conversão demorou > 60s | Aumentar timeout ou verificar FFmpeg |

## Performance

### Tempos Esperados (com FFmpeg instalado)

| Operação | Tamanho | Tempo Estimado |
|----------|---------|----------------|
| WebM/Opus → AMR (10s áudio) | ~100 KB | ~500ms - 1s |
| AMR → MP3 (10s áudio) | ~5 KB | ~300ms - 500ms |

### Otimizações

1. **Conversão sob demanda**: MP3 é gerado apenas quando solicitado
2. **Limpeza automática**: Arquivos temporários são removidos imediatamente após conversão
3. **Parallelização**: Conversões podem ocorrer em paralelo para diferentes usuários

## Testes

### Testes Unitários

Localização: `src/test/java/br/com/zontec/watchmonitor/server/service/AudioConverterServiceTest.java`

Testes implementados:
- ✅ Rejeição de entrada nula
- ✅ Rejeição de entrada vazia
- ✅ Criação automática de diretório temporário
- ✅ Simulação de conversão com FFmpeg

### Executar Testes

```bash
mvn test -Dtest=AudioConverterServiceTest
```

### Testes Manuais

#### 1. Testar Conversão WebM → AMR

```bash
# Gravar áudio com navegador
# Enviar para endpoint:
curl -X POST \
  -F "file=@audio.webm" \
  http://localhost:8080/api/devices/861265061271273/audios/send
```

#### 2. Testar Conversão AMR → MP3

```bash
# Após receber áudio do dispositivo, fazer stream:
curl http://localhost:8080/api/device-audios/1/stream -o audio.mp3
# Reproduzir: mpv audio.mp3 (ou player de sua preferência)
```

## Troubleshooting

### Erro: "Erro ao executar FFmpeg"

**Solução**:
```bash
# Verificar instalação
ffmpeg -version

# Se não encontrado, instalar:
# Ubuntu/Debian
sudo apt-get install ffmpeg

# macOS
brew install ffmpeg

# Windows - baixar de https://ffmpeg.org/download.html
# e adicionar ao PATH
```

### Erro: "Conversão expirou (timeout)"

**Solução**:
- Aumentar o timeout em `AudioConverterService.executeFFmpegCommand()` (linha ~130)
- Verificar recursos do sistema (CPU, disco)

### Erro: "Arquivo de saída não foi criado"

**Solução**:
- Verificar permissões de escrita em `audio.temp.directory`
- Verificar espaço em disco disponível
- Testar manualmente: `ffmpeg -i input.webm -acodec libopencore_amrnb -ac 1 -ar 8000 -ab 12.2k output.amr -y`

### Performance Lenta

**Solução**:
- Aumentar recursos de CPU/RAM
- Reduzir qualidade: modificar bitrate em `AudioConverterService` (linha ~150, ~155)
- Implementar fila de processamento com Spring Task Executor

## Próximas Melhorias

1. **Cache de conversões**: Reutilizar conversões já realizadas
2. **Processamento assíncrono**: Usar Spring Async para não bloquear requisição
3. **Suporte a outros formatos**: WAV, FLAC, OGG
4. **Métricas**: Registrar tempo de conversão, tamanho, erros
5. **Validação de áudio**: Verificar qualidade do arquivo antes de converter

## Referências

- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [FFmpeg4j GitHub](https://github.com/bramp/ffmpeg-cli-wrapper)
- [AMR Audio Format](https://en.wikipedia.org/wiki/Adaptive_Multi-Rate_audio_codec)
- [MP3 Audio Format](https://en.wikipedia.org/wiki/MP3)
