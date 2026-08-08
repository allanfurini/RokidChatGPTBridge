# Rokid ChatGPT Bridge v0.3

Protótipo Android para usar o **ChatGPT oficial no celular** e o **Rokid Glasses como voz/HUD**, sem chave da API da OpenAI.

## Fluxo pretendido

Rokid Glasses → fala → Rokid Nexus STT → celular → aplicativo oficial ChatGPT
→ Accessibility captura a conversa visível → Rokid Nexus → HUD dos óculos.

## O que esta versão testa

- integração como plugin do Rokid Nexus;
- Speech-to-text do Nexus;
- abertura do app oficial do ChatGPT;
- tentativa de inserir a fala na conversa aberta via Accessibility;
- captura do texto visível do ChatGPT;
- retorno do texto ao HUD;
- TTS opcional pelo Nexus.

## Importante

Esta é uma **versão de teste**, não uma versão final. A parte mais provável de precisar de ajuste é a automação da interface do aplicativo ChatGPT, porque a árvore de acessibilidade pode variar entre versões do app.

## Compilação

O projeto já contém `.github/workflows/build-apk.yml`.
Ele usa GitHub Actions e **não depende de `gradlew` nem de Gradle Wrapper no ZIP**.
