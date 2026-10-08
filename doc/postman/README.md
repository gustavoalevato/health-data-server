# Postman Collection

Collection pronta:

- `doc/postman/Thalamus-Watch-Monitor.postman_collection.json`

Para regenerar a collection diretamente do Swagger/OpenAPI exposto pela aplicacao:

```bash
python3 scripts/export_openapi_to_postman.py http://localhost:8080/v3/api-docs doc/postman/Thalamus-Watch-Monitor.postman_collection.json
```

Autenticacao:

- `POST /api/auth/token`: gera um `accessToken` usando as credenciais configuradas em `app.security.username` e `app.security.password`.
- A collection usa Bearer Token no nivel global. Depois de gerar o token, preencha a variavel `{{accessToken}}`.

Endpoints de audio:

- `POST /api/devices/{{imei}}/audios/send`: envia um arquivo AMR gravado para o dispositivo Touch4P usando o comando TK. No Postman, use `Body > form-data`, campo `file`, tipo `File`.
- `GET /api/devices/{{imei}}/audios?limit=20`: lista os ultimos audios recebidos do dispositivo.
- `GET /api/device-audios/{{audioId}}/stream`: faz stream de um audio recebido.

Endpoints de saude:

- `GET /api/health/{{imei}}/comprehensive-health`: lista medicoes consolidadas de saude.
- `GET /api/health/{{imei}}/heart-rates`: lista batimentos cardiacos.
- `GET /api/health/{{imei}}/body-temperatures`: lista temperaturas corporais.
- `GET /api/health/{{imei}}/blood-pressures`: lista pressoes arteriais.
- `GET /api/health/{{imei}}/sleep-sessions`: lista sessoes de sono.
- `GET /api/health/{{imei}}/ecg-data`: lista leituras de ECG.
- `GET /api/health/{{imei}}/heartbeats`: lista heartbeats.
- `GET /api/health/{{imei}}/oxygen-levels`: lista registros de SpO2/oxigenio.
- `GET /api/health/{{imei}}/locations`: lista localizacoes.
- `GET /api/health/{{imei}}/fall-down-alerts`: lista alertas de queda.
- `GET /api/health/{{imei}}/sleep-entries`: lista entradas de sono.

Observacoes:

- A collection usa as variaveis `{{baseUrl}}`, `{{imei}}`, `{{deviceId}}`, `{{audioId}}` e `{{accessToken}}`.
- O endpoint Swagger configurado no projeto e `http://localhost:8080/v3/api-docs`.
- Se a API estiver em outra porta ou host, ajuste apenas `{{baseUrl}}` no Postman.
