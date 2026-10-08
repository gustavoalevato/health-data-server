#!/bin/bash

# Script de instalação do FFmpeg com suporte a AMR
# Funciona em: Linux (Debian/Ubuntu), macOS e permite manual para Windows

set -e

echo "======================================"
echo "Instalador de FFmpeg com suporte AMR"
echo "======================================"
echo ""

# Detectar sistema operacional
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "Sistema detectado: Linux"
    
    # Verificar se é Debian/Ubuntu
    if command -v apt-get &> /dev/null; then
        echo "Gerenciador de pacotes: apt"
        echo ""
        echo "Instalando FFmpeg com suporte a AMR..."
        
        # Atualizar lista de pacotes
        sudo apt-get update
        
        # Instalar FFmpeg e codec AMR
        sudo apt-get install -y ffmpeg libopus-dev libvorbis-dev libtheora-dev libvpx-dev libx264-dev libx265-dev opencore-amr
        
        echo ""
        echo "✅ FFmpeg instalado com sucesso!"
        
    elif command -v dnf &> /dev/null; then
        echo "Gerenciador de pacotes: dnf (Fedora/RHEL)"
        echo ""
        echo "Instalando FFmpeg com suporte a AMR..."
        
        sudo dnf install -y ffmpeg libopus-devel libvorbis-devel libtheora-devel libvpx-devel x264-devel x265-devel opencore-amr
        
        echo ""
        echo "✅ FFmpeg instalado com sucesso!"
        
    else
        echo "❌ Gerenciador de pacotes não identificado"
        echo "Por favor, instale FFmpeg manualmente com suporte a AMR"
        exit 1
    fi

elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Sistema detectado: macOS"
    echo ""
    
    if command -v brew &> /dev/null; then
        echo "Gerenciador de pacotes: Homebrew"
        echo ""
        echo "Instalando FFmpeg com suporte a AMR..."
        
        # Instalar FFmpeg com todas as opções de codec
        brew install ffmpeg --with-opencore-amr
        
        # Se a opção --with-opencore-amr não funcionar (versões recentes), instalar separado
        if ! ffmpeg -encoders 2>&1 | grep -i opencore_amr &> /dev/null; then
            echo "Instalando opencore-amr separadamente..."
            brew install opencore-amr
        fi
        
        echo ""
        echo "✅ FFmpeg instalado com sucesso!"
        
    else
        echo "❌ Homebrew não encontrado"
        echo "Instale Homebrew primeiro: https://brew.sh/"
        exit 1
    fi

elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    echo "Sistema detectado: Windows"
    echo ""
    echo "⚠️  Instalação manual necessária"
    echo ""
    echo "Opção 1: Usando Chocolatey (recomendado)"
    echo "  choco install ffmpeg"
    echo ""
    echo "Opção 2: Download manual"
    echo "  1. Acesse: https://ffmpeg.org/download.html"
    echo "  2. Baixe uma versão compilada com suporte AMR (ex: BtbN builds)"
    echo "  3. Extraia em C:\\ffmpeg"
    echo "  4. Adicione ao PATH: C:\\ffmpeg\\bin"
    echo ""
    echo "Verificar: ffmpeg -version"
    exit 1

else
    echo "❌ Sistema operacional não reconhecido: $OSTYPE"
    exit 1
fi

# Verificar instalação
echo ""
echo "Verificando instalação..."
echo ""
ffmpeg -version | head -5
echo ""

# Verificar suporte a AMR
echo "Verificando suporte a AMR..."
if ffmpeg -encoders 2>&1 | grep -i "opencore_amr\|libopencore_amrnb" &> /dev/null; then
    echo "✅ Codec AMR (libopencore_amrnb) encontrado!"
else
    echo "⚠️  Codec AMR não encontrado no FFmpeg"
    echo ""
    echo "Isso pode significar que:"
    echo "1. O FFmpeg foi compilado sem suporte a AMR"
    echo "2. A instalação ainda está em andamento"
    echo ""
    echo "Soluções:"
    echo "- Para Linux: apt-get install opencore-amr"
    echo "- Para macOS: brew install opencore-amr"
    echo "- Para Windows: Use BtbN builds (https://github.com/BtbN/FFmpeg-Builds)"
fi

# Verificar suporte a MP3
echo ""
echo "Verificando suporte a MP3..."
if ffmpeg -encoders 2>&1 | grep -i "libmp3lame" &> /dev/null; then
    echo "✅ Codec MP3 (libmp3lame) encontrado!"
else
    echo "⚠️  Codec MP3 não encontrado"
fi

# Verificar suporte a Opus
echo ""
echo "Verificando suporte a Opus (WebM)..."
if ffmpeg -encoders 2>&1 | grep -i "libopus\|opus" &> /dev/null; then
    echo "✅ Codec Opus encontrado!"
else
    echo "⚠️  Codec Opus não encontrado"
fi

echo ""
echo "======================================"
echo "✅ Instalação concluída!"
echo "======================================"
