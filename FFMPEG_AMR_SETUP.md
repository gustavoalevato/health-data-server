# FFmpeg com Suporte AMR - Guia de Instalação

## Problema

O erro `Unknown encoder 'libopencore_amrnb'` ocorre quando o FFmpeg instalado no seu sistema não foi compilado com suporte ao codec AMR (Adaptive Multi-Rate). Este é um codec importante para compatibilidade com dispositivos móveis e relógios inteligentes como o Touch4P.

## Solução: Instalar FFmpeg com Suporte AMR

### 1️⃣ Desenvolvimento Local (Linux/macOS)

#### Método Automático (Recomendado)

Execute o script de instalação:

```bash
# Tornar script executável (se necessário)
chmod +x scripts/install-ffmpeg-amr.sh

# Executar script
./scripts/install-ffmpeg-amr.sh
```

O script detectará seu SO e instalará automaticamente as dependências.

#### Método Manual - Ubuntu/Debian

```bash
# Atualizar lista de pacotes
sudo apt-get update

# Instalar FFmpeg com suporte AMR
sudo apt-get install -y \
    ffmpeg \
    libopus-dev \
    libvorbis-dev \
    libtheora-dev \
    libvpx-dev \
    libx264-dev \
    libx265-dev \
    opencore-amr

# Verificar instalação
ffmpeg -encoders | grep opencore_amrnb
```

#### Método Manual - Fedora/RHEL

```bash
sudo dnf install -y \
    ffmpeg \
    libopus-devel \
    libvorbis-devel \
    libtheora-devel \
    libvpx-devel \
    x264-devel \
    x265-devel \
    opencore-amr
```

#### Método Manual - macOS

```bash
# Usando Homebrew
brew install ffmpeg --with-opencore-amr

# Se a opção acima não funcionar (versões recentes do brew)
brew install opencore-amr
brew install ffmpeg

# Verificar instalação
ffmpeg -encoders | grep opencore_amrnb
```

### 2️⃣ Docker (Production)

O Dockerfile foi atualizado para instalar FFmpeg com suporte AMR automaticamente:

```dockerfile
# Instala FFmpeg com AMR no container
RUN apk add --no-cache \
    ffmpeg \
    libmp3lame \
    libvorbis \
    libopus \
    libtheora \
    libvpx \
    libx264 \
    libx265 \
    opencore-amr
```

Para reconstruir a imagem com as novas dependências:

```bash
# Rebuild Docker image
docker build -t thalamus-watch-monitor:latest .

# Run container
docker run -p 5088:5088 -p 8080:8080 thalamus-watch-monitor:latest
```

### 3️⃣ Windows

#### Opção A: Usando Chocolatey (Recomendado)

```bash
# No PowerShell como Administrador
choco install ffmpeg
```

#### Opção B: Download Manual

1. Acesse: https://github.com/BtbN/FFmpeg-Builds/releases
2. Baixe a versão mais recente (ex: `ffmpeg-full-gpl-N-xxxxx`)
3. Extraia em um local permanente (ex: `C:\ffmpeg`)
4. Adicione ao PATH do Windows:
   - Abra "Variáveis de Ambiente"
   - Adicione `C:\ffmpeg\bin` ao PATH
5. Verifique: `ffmpeg -version`

## ✅ Verificar Instalação

```bash
# Verificar versão
ffmpeg -version

# Verificar codecs AMR
ffmpeg -encoders | grep -i "amr\|opencore"

# Saída esperada:
# EA..... libopencore_amrnb    libopn core AMR (Narrowband) (codec amr_nb)

# Verificar codecs MP3
ffmpeg -encoders | grep -i "mp3"

# Saída esperada:
# A..... libmp3lame           libmp3lame MP3 (MPEG audio layer 3)

# Testar conversão WebM → AMR
ffmpeg -i input.webm -acodec libopencore_amrnb -ac 1 -ar 8000 -ab 12.2k output.amr

# Testar conversão AMR → MP3
ffmpeg -i input.amr -acodec libmp3lame -q:a 4 output.mp3
```

## 🔧 Solução de Problemas

### Erro: "Unknown encoder 'libopencore_amrnb'"

**Causa**: FFmpeg compilado sem suporte AMR

**Soluções**:
1. Instalar `opencore-amr` separadamente: `sudo apt-get install opencore-amr`
2. Recompilar FFmpeg com suporte AMR
3. Usar versão pré-compilada com suporte total

### Erro: "libopencore_amrnb not found"

**Solução**:
```bash
# Linux
sudo apt-get install libopencore-amr0 opencore-amr

# macOS
brew install opencore-amr
```

### Erro: "ffmpeg: command not found"

**Solução**:
```bash
# Verificar instalação
which ffmpeg

# Se não encontrar, adicionar ao PATH
echo 'export PATH="/usr/local/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

## 🧪 Testar Aplicação

### 1. Iniciar servidor

```bash
cd /caminho/para/servidor
mvn clean spring-boot:run
```

### 2. Testar conversão WebM → AMR

```bash
# Criar arquivo de teste WebM (assumindo que tem ffmpeg)
# (Este é um arquivo de exemplo)
ffmpeg -f lavfi -i sine=frequency=1000:duration=2 -c:a libopus test.webm -y

# Enviar para API
curl -X POST \
  -F "file=@test.webm" \
  http://localhost:8080/api/devices/861265061271273/audios/send
```

### 3. Testar conversão AMR → MP3

```bash
# Criar arquivo de teste AMR
ffmpeg -f lavfi -i sine=frequency=1000:duration=2 -acodec libopencore_amrnb -ac 1 -ar 8000 -ab 12.2k test.amr -y

# Obter stream MP3 (substitua 1 pelo ID real do áudio)
curl http://localhost:8080/api/device-audios/1/stream -o output.mp3

# Reproduzir
mpv output.mp3
```

## 📋 Checklist de Verificação

- [ ] FFmpeg instalado: `ffmpeg -version`
- [ ] Codec AMR disponível: `ffmpeg -encoders | grep opencore_amrnb`
- [ ] Codec MP3 disponível: `ffmpeg -encoders | grep libmp3lame`
- [ ] Teste WebM → AMR realizado com sucesso
- [ ] Teste AMR → MP3 realizado com sucesso
- [ ] Servidor iniciado sem erros
- [ ] Endpoint de envio de áudio funcionando
- [ ] Endpoint de stream de áudio funcionando

## 🔗 Referências

- [FFmpeg Downloads](https://ffmpeg.org/download.html)
- [FFmpeg Builds (Windows)](https://github.com/BtbN/FFmpeg-Builds)
- [OpenCore-amr](https://opencore-amr.sourceforge.io/)
- [AMR Audio Codec](https://en.wikipedia.org/wiki/Adaptive_Multi-Rate_audio_codec)

## 📞 Suporte

Se tiver problemas, execute:

```bash
# Capturar informações de debug
ffmpeg -version > ffmpeg_info.txt
ffmpeg -encoders >> ffmpeg_info.txt
ffmpeg -decoders >> ffmpeg_info.txt

# Testar conversão com verbose
ffmpeg -v verbose -i input.webm -acodec libopencore_amrnb -ac 1 -ar 8000 -ab 12.2k output.amr -y 2>&1 | tee conversion.log
```

Compartilhe os arquivos `ffmpeg_info.txt` e `conversion.log` para debug adicional.
