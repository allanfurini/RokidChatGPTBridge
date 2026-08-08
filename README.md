# Rokid ChatGPT Bridge v0.6 — Hi Rokid CXR-L direto

Esta versão deixa de depender do Rokid Nexus e usa diretamente o aplicativo que está no telefone:

`com.rokid.sprite.global.aiapp`

## Objetivo do teste

1. Detectar Hi Rokid.
2. Pedir autorização oficial do Hi Rokid.
3. Abrir uma sessão CXR-L `CUSTOMVIEW`.
4. Mostrar texto diretamente no HUD.
5. Receber PCM 16 kHz / mono / 16-bit do microfone dos óculos.
6. Injetar esse PCM no reconhecimento de fala Android usando o pipe `EXTRA_AUDIO_SOURCE`.
7. Colocar o texto reconhecido no ChatGPT oficial via Accessibility.

Não usa a API da OpenAI.

## Ordem do teste

1. Conecte os Rokid Glasses normalmente no Hi Rokid.
2. Abra Rokid ChatGPT Bridge.
3. Ative Acessibilidade.
4. Toque `Autorizar no Hi Rokid` e aceite a tela de confiança.
5. Aguarde aparecer `CXR-L conectado`.
6. Teste HUD.
7. Teste microfone.
8. Se a fala for transcrita, o app abrirá o ChatGPT oficial e tentará inserir a pergunta.
