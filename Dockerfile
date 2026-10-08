FROM jrottenberg/ffmpeg:8-ubuntu AS ffmpeg

FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .

RUN mvn dependency:go-offline -B
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

# Instala tzdata e outras dependências
RUN apt-get update && apt-get install -y tzdata && \
    ln -snf /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime && \
    echo "America/Sao_Paulo" > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copia FFmpeg da imagem Ubuntu
COPY --from=ffmpeg / /

# Encontra onde as bibliotecas foram instaladas e adiciona ao PATH
RUN find / -name "libavdevice.so*" 2>/dev/null || echo "Bibliotecas não encontradas"

# Adiciona os diretórios comuns de bibliotecas ao LD_LIBRARY_PATH
ENV LD_LIBRARY_PATH=/usr/local/lib:/usr/lib:/lib:$LD_LIBRARY_PATH

# Garante permissão de execução
RUN chmod +x /usr/local/bin/ffmpeg /usr/local/bin/ffprobe

# Testa se consegue encontrar as bibliotecas
RUN ldd /usr/local/bin/ffmpeg | head -20

ENV TZ=America/Sao_Paulo  
ENV JAVA_OPTS="-Duser.timezone=$TZ"

RUN groupadd -r appgroup -g 1001 && \
    useradd -r -g appgroup -u 1001 -m -s /sbin/nologin appuser

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 5088

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD pgrep java || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]