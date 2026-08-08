# Rokid ChatGPT Bridge v0.2

## O que este projeto tenta fazer

Ele NÃO usa a API da OpenAI.

O fluxo é:

1. Rokid Nexus recebe sua voz pelo microfone dos Rokid Glasses.
2. O Nexus converte a fala em texto no telefone.
3. O plugin abre o aplicativo oficial do ChatGPT.
4. Um Accessibility Service coloca o texto na caixa da conversa atual e tenta enviar.
5. O serviço acompanha o texto visível da conversa.
6. A resposta é enviada para o HUD do Rokid pelo Nexus.
7. Opcionalmente, o Nexus lê a resposta nos óculos.

Isso preserva o uso do aplicativo oficial e permite continuar a conversa que estiver aberta no celular.

## Requisito importante

Instale primeiro o Rokid Nexus e conecte os óculos. O SDK público do Nexus fornece:
- HUD;
- botão AI;
- speech-to-text usando o microfone dos óculos;
- text-to-speech.

## Teste v0.2

Esta é uma versão de TESTE. O ponto mais sujeito a ajuste é a automação da interface do ChatGPT, porque os elementos de acessibilidade do aplicativo podem mudar entre versões.

O que testar:
1. Abra no celular a conversa do ChatGPT que quer continuar.
2. Ative a acessibilidade "Rokid ChatGPT Bridge".
3. No Nexus, aprove ChatGPT Bridge e conceda Surfaces, Speech-to-text e Text-to-speech.
4. Abra ChatGPT Bridge no HUD.
5. Toque no centro ou use o botão de IA.
6. Diga uma frase curta.
7. Veja se:
   - a frase aparece no HUD;
   - o ChatGPT abre no celular;
   - a frase é inserida/enviada;
   - a resposta volta para o HUD.

## Se falhar

Envie apenas:
- print da tela do celular;
- foto/print do HUD;
- diga em qual etapa parou: voz / abriu ChatGPT / digitou / enviou / resposta voltou.

NUNCA envie senha, token, chave ou cookie.
