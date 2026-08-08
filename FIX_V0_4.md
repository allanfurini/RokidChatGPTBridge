# v0.4 — correção do SDK Nexus

Esta versão substitui os imports incorretos `com.anezium.rokidbus.sdk.*` pelos
pacotes reais publicados pelo SDK:

- `com.anezium.rokidbus.client.plugin.*`
- `com.anezium.rokidbus.shared.plugin.NexusInputEvent`

Também aplica o contrato headless real do plugin Nexus:
- sem MAIN/LAUNCHER;
- FGS specialUse;
- query do HUB;
- capabilities `surfaces,stt,tts`;
- receive prefixes compatíveis;
- STT em português (`pt`);
- HUD via `NexusCard`;
- TTS via `NexusTtsSession`.
