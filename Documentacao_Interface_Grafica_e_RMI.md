# Documentacao da Interface Grafica e Perguntas Respondidas sobre Java RMI

Projeto: **Jogo 21 / Blackjack distribuido em Java**

Este documento explica como a interface grafica funciona depois da separacao entre cliente RMI e tela Swing, como o Java RMI e usado no projeto, e responde perguntas importantes para apresentacao, defesa ou estudo.

## 1. Visao geral do sistema

O projeto implementa uma mesa compartilhada de Jogo 21. O servidor mantem o estado real do jogo, enquanto cada cliente exibe uma interface grafica em Swing e faz chamadas remotas para jogar.

A regra principal da arquitetura e:

- `Servidor` e `Jogo21InterfaceImpl`: controlam o jogo.
- `Cliente`: controla a comunicacao RMI e liga a tela ao servidor.
- `ClienteGUI`: controla toda a interface grafica.
- `EstadoPartida`, `EstadoJogador` e `Carta`: transportam dados entre servidor e cliente.

Com a refatoracao, o arquivo `Cliente.java` nao monta mais a interface diretamente. Ele apenas cria um objeto `ClienteGUI`, registra os eventos da tela e executa as chamadas remotas.

## 2. Principais arquivos

| Arquivo | Responsabilidade |
|---|---|
| `Servidor.java` | Inicia o RMI Registry na porta `1099` e registra o servico `ServicoJogo21`. |
| `Jogo21Interface.java` | Define os metodos remotos que o cliente pode chamar. |
| `Jogo21InterfaceImpl.java` | Implementa as regras do jogo, guarda jogadores, cartas, turnos, apostas e resultados. |
| `Cliente.java` | Controlador do cliente: cuida de RMI, callback, `SwingWorker`, timer e ligacao dos botoes. |
| `ClienteGUI.java` | Janela Swing: monta a tela, desenha cartas, mostra jogadores, status, erros e resultado da rodada. |
| `ClienteCallback.java` | Interface remota usada pelo servidor para notificar clientes. |
| `ClienteCallbackImpl.java` | Callback simples para console, util para testes. |
| `EstadoPartida.java` | Estado serializavel da partida visto por um jogador especifico. |
| `EstadoJogador.java` | Resumo serializavel de um jogador para a barra lateral. |
| `Carta.java` | Representa uma carta do baralho e e enviada por RMI como objeto serializavel. |

## 3. Nova separacao entre `Cliente` e `ClienteGUI`

Antes, `Cliente.java` acumulava duas responsabilidades:

1. Fazer a comunicacao RMI.
2. Criar e atualizar a interface grafica.

Agora essas responsabilidades foram separadas.

### `Cliente.java`

O `Cliente` e o controlador da aplicacao. Ele nao herda mais de `JFrame`. Suas responsabilidades sao:

- criar uma instancia de `ClienteGUI`;
- iniciar a interface pela thread correta do Swing;
- conectar ao RMI Registry;
- buscar o objeto remoto `ServicoJogo21`;
- chamar `conectar`, `pedirCarta`, `pararJogada`, `obterEstado`, `listarJogadores` e `desconectar`;
- criar o callback remoto `GuiCallback`;
- usar `SwingWorker` para nao travar a tela durante chamadas remotas;
- usar um `Timer` para atualizar periodicamente a mesa;
- receber eventos da GUI e transformar esses eventos em chamadas RMI.

Trecho central:

```java
private final ClienteGUI gui = new ClienteGUI();
private final Timer playersRefreshTimer = new Timer(1500, event -> refreshTableAndState());
```

Isso mostra que `Cliente` usa a interface grafica, mas nao implementa os componentes visuais.

### `ClienteGUI.java`

O `ClienteGUI` e a janela grafica. Ele herda de `JFrame` e contem:

- campos `hostField`, `portField` e `nameField`;
- botoes `connectButton`, `leaveButton`, `hitButton` e `standButton`;
- labels de status, identidade, pontuacao e fichas;
- paineis de cartas do dealer e do jogador;
- lista lateral de jogadores;
- area de log;
- componentes personalizados de cartas.

Ele tambem fornece metodos publicos para o controlador usar, por exemplo:

```java
gui.onConnect(event -> connect());
gui.renderState(currentState);
gui.renderPlayersTable(jogadores, playerName);
gui.showRoundResultAnimation(resumo);
```

Assim, a tela nao precisa conhecer detalhes de RMI, e o cliente RMI nao precisa conhecer detalhes de desenho da tela.

## 4. Fluxo inicial da aplicacao

O metodo `main` esta em `Cliente.java`:

```java
public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new Cliente().show());
}
```

O fluxo inicial e:

1. `main()` chama `SwingUtilities.invokeLater(...)`.
2. Uma instancia de `Cliente` e criada.
3. O `Cliente` cria internamente uma instancia de `ClienteGUI`.
4. O construtor do `Cliente` chama `bindActions()`.
5. `bindActions()` liga os botoes da tela aos metodos RMI.
6. `show()` exibe a janela.

O uso de `SwingUtilities.invokeLater` e importante porque componentes Swing devem ser criados e alterados na thread correta da interface grafica.

## 5. Areas principais da interface

Toda a montagem visual esta em `ClienteGUI.java`.

| Area | Metodo em `ClienteGUI` | Funcao |
|---|---|---|
| Barra superior | `buildTopBar()` | Mostra titulo, identidade do jogador, host, porta, conectar e sair da mesa. |
| Mesa central | `buildTable()` | Mostra cartas do dealer, status, log e cartas do jogador. |
| Barra lateral | `buildPlayersSidebar()` | Lista jogadores conectados, status, aposta, historico e cartas visiveis. |
| Barra inferior | `buildBottomBar()` | Mostra botoes `Pedir carta`, `Parar` e informacoes de fichas. |

### Barra superior

A barra superior usa `BorderLayout`.

Do lado esquerdo:

- titulo `Jogo 21`;
- identidade do jogador.

Do lado direito:

- campo `Host`;
- campo `Porta`;
- botao `Conectar`;
- botao `Sair da mesa`.

Antes da conexao, a identidade aparece como:

```text
Voce ainda nao entrou na mesa
```

Depois da conexao:

```text
Jogador: NomeEscolhido
```

### Mesa central

A mesa central usa `GridBagLayout` e organiza os elementos verticalmente:

1. Cabecalho do dealer.
2. Cartas do dealer.
3. Status e log.
4. Cabecalho da mao do jogador.
5. Cartas do jogador.

Enquanto a rodada nao termina, o dealer mostra apenas uma carta real e uma carta virada para baixo.

### Barra lateral

A barra lateral mostra todos os jogadores conectados. O proprio jogador e destacado com:

- fundo verde mais claro;
- borda dourada;
- prefixo `VOCE`.

Os outros jogadores nao tem suas cartas reais reveladas. Quando participam da rodada, aparecem cartas pequenas ocultas.

### Barra inferior

A barra inferior contem:

- `Pedir carta`;
- `Parar`;
- informacoes de fichas, aposta e historico.

Exemplo:

```text
Fichas: 90 | Aposta: 10 | Historico: 1V/0E/2D
```

## 6. Como os botoes chegam ao RMI

Em `ClienteGUI`, os botoes ficam privados. Para o controlador usar esses botoes, a GUI oferece metodos publicos:

```java
public void onConnect(ActionListener listener)
public void onLeaveTable(ActionListener listener)
public void onHit(ActionListener listener)
public void onStand(ActionListener listener)
public void onWindowClosing(Runnable action)
```

Em `Cliente.java`, esses eventos sao ligados assim:

```java
gui.onConnect(event -> connect());
gui.onLeaveTable(event -> leaveTable());
gui.onHit(event -> runRemoteAction("Pedindo carta...", () -> service.pedirCarta(playerName)));
gui.onStand(event -> runRemoteAction("Dealer jogando...", () -> service.pararJogada(playerName)));
gui.onWindowClosing(this::disconnectFromServer);
```

Isso deixa claro que a GUI apenas avisa que algo aconteceu. Quem decide chamar RMI e o `Cliente`.

## 7. Conexao com o servidor

Quando o usuario clica em `Conectar`, o metodo `connect()` de `Cliente.java` executa:

1. Pede o nome do jogador usando `gui.promptPlayerName()`.
2. Le host e porta com `gui.getHost()` e `gui.getPort()`.
3. Desabilita temporariamente a tela com `gui.prepareConnecting()`.
4. Usa `SwingWorker` para executar a comunicacao remota.
5. Acessa o registry:

```java
Registry registry = LocateRegistry.getRegistry(host, port);
```

6. Busca o servico:

```java
service = (Jogo21Interface) registry.lookup("ServicoJogo21");
```

7. Cria o callback:

```java
callback = new GuiCallback();
```

8. Cria um identificador unico:

```java
playerName = playerDisplayName + "-" + UUID.randomUUID().toString().substring(0, 8);
```

9. Chama o servidor:

```java
service.conectar(playerName, playerDisplayName, callback);
```

O identificador unico evita conflito quando dois jogadores escolhem o mesmo nome.

## 8. Por que o cliente usa `SwingWorker`

Chamadas RMI podem demorar, porque dependem de rede, servidor e serializacao de objetos. Se uma chamada remota fosse executada diretamente na thread da interface, a janela poderia congelar.

Por isso, o `Cliente` usa `SwingWorker` para:

- conectar;
- pedir carta;
- parar;
- listar jogadores;
- obter estado atualizado.

O trabalho remoto fica em `doInBackground()`, e a atualizacao da tela fica em `done()`.

## 9. Atualizacao da tela

O metodo central da GUI e:

```java
public void renderState(EstadoPartida state)
```

Ele recebe o estado calculado pelo servidor e atualiza:

- cartas do dealer;
- cartas do jogador;
- pontuacao do jogador;
- pontuacao do dealer;
- fichas;
- aposta;
- historico;
- mensagem principal;
- log.

O servidor envia `EstadoPartida`, e a GUI transforma esse objeto em tela.

## 10. Exibicao das cartas

As cartas sao desenhadas por classes internas de `ClienteGUI.java`, nao por imagens externas.

| Classe | Funcao |
|---|---|
| `CardView` | Carta grande usada na mesa principal. |
| `MiniCardView` | Carta pequena usada na barra lateral. |
| `HiddenMiniCardView` | Carta oculta usada para dealer ou outros jogadores. |

As cores seguem a regra:

- copas e ouros: vermelho;
- paus e espadas: preto.

Cartas abertas usam fundo claro. Cartas ocultas usam fundo azul em gradiente e simbolo de espadas.

## 11. Estados mostrados na interface

O enum `EstadoPartida.Status` define a situacao do jogador na rodada.

| Status | Mensagem principal |
|---|---|
| `EM_ANDAMENTO` | Sua vez: escolha pedir carta ou parar. |
| `JOGADOR_ESTOUROU` | Voce estourou. Dealer venceu. |
| `DEALER_ESTOUROU` | Dealer estourou. Voce venceu! |
| `JOGADOR_VENCEU` | Voce venceu! |
| `DEALER_VENCEU` | Dealer venceu. |
| `AGUARDANDO_PROXIMA_RODADA` | Mesa em andamento. Voce entra na proxima rodada. |
| `AGUARDANDO_VEZ` | Aguarde sua vez. |
| `JOGADOR_PAROU` | Voce parou. Aguardando o fim da rodada. |
| `EMPATE` | Empate. |

A conversao e feita em `ClienteGUI.messageForStatus(...)`.

## 12. Callback RMI

O callback permite o caminho inverso da comunicacao.

Normalmente:

```text
Cliente chama Servidor
```

Com callback:

```text
Servidor chama Cliente
```

No projeto, o callback e a interface:

```java
void notificar(String mensagem) throws RemoteException;
```

Em `Cliente.java`, a classe interna `GuiCallback` implementa `ClienteCallback` e estende `UnicastRemoteObject`.

Quando o servidor envia uma mensagem comum, o cliente adiciona no log:

```text
[Servidor] mensagem
```

Quando a mensagem comeca com:

```text
RESULTADO_RODADA|
```

o cliente extrai o resumo e chama:

```java
gui.showRoundResultAnimation(resumo);
```

Esse metodo abre uma janela temporaria de resultado por 5 segundos.

## 13. Atualizacao periodica

O `Cliente` possui um timer:

```java
private final Timer playersRefreshTimer = new Timer(1500, event -> refreshTableAndState());
```

A cada 1500 ms, ele:

1. Chama `listarJogadores(...)`.
2. Chama `obterEstado(...)`.
3. Atualiza a GUI.
4. Habilita ou desabilita botoes.

Isso ajuda a manter os clientes sincronizados mesmo quando outro jogador age.

## 14. O que fica no servidor

O servidor e a autoridade do jogo. Ele decide:

- jogadores conectados;
- participantes da rodada;
- ordem dos turnos;
- baralho;
- cartas dos jogadores;
- cartas do dealer;
- pontuacao;
- resultado;
- saldo;
- aposta;
- vitorias, empates e derrotas.

O cliente nao altera regra localmente. Ele apenas solicita acoes e mostra o estado recebido.

## 15. Fluxo completo de uma jogada

Exemplo: jogador clica em `Pedir carta`.

1. `ClienteGUI` detecta o clique no botao.
2. `Cliente` recebe o evento registrado em `gui.onHit(...)`.
3. `Cliente` chama:

```java
runRemoteAction("Pedindo carta...", () -> service.pedirCarta(playerName))
```

4. A GUI desabilita temporariamente os botoes.
5. Um `SwingWorker` executa a chamada remota.
6. O servidor valida se e a vez do jogador.
7. O servidor compra a carta e recalcula a pontuacao.
8. O servidor muda o status se o jogador estourar.
9. O servidor retorna um novo `EstadoPartida`.
10. O `Cliente` chama `gui.renderState(currentState)`.
11. A tela mostra carta, pontuacao e status atualizados.

## 16. Vantagens da refatoracao

A separacao entre `Cliente` e `ClienteGUI` melhora o projeto porque:

- reduz o tamanho e a complexidade de `Cliente.java`;
- separa comunicacao remota de interface grafica;
- facilita explicar o trabalho;
- facilita testar ou alterar a GUI sem mexer no RMI;
- facilita criar outro tipo de cliente no futuro, como console ou web;
- deixa cada classe com uma responsabilidade mais clara.

## 17. Perguntas respondidas sobre Java RMI

### Perguntas conceituais

1. **O que e Java RMI?**  
   Java RMI, ou Remote Method Invocation, e uma tecnologia que permite chamar metodos de objetos Java que estao em outra JVM, possivelmente em outra maquina, como se fossem metodos locais.

2. **Por que Java RMI foi adequado para este trabalho?**  
   Porque o jogo tem um servidor central e varios clientes Java. O RMI permite que cada cliente chame metodos do servidor, como `pedirCarta` e `pararJogada`, sem implementar manualmente sockets e protocolos de mensagem.

3. **Qual e a funcao de uma interface que estende `Remote`?**  
   Ela define o contrato remoto. No projeto, `Jogo21Interface` diz quais metodos podem ser chamados remotamente pelo cliente.

4. **Por que todos os metodos remotos declaram `RemoteException`?**  
   Porque uma chamada remota pode falhar por problemas de rede, servidor indisponivel, objeto remoto inacessivel ou erro de comunicacao.

5. **Qual e a diferenca entre enviar um objeto serializavel e enviar uma referencia remota?**  
   Um objeto serializavel e enviado por valor, ou seja, o outro lado recebe uma copia. Uma referencia remota permite chamar metodos no objeto original que continua em outra JVM.

6. **Por que `Carta`, `EstadoPartida` e `EstadoJogador` implementam `Serializable`?**  
   Porque esses objetos sao enviados entre servidor e cliente por RMI como dados. O cliente recebe copias desses objetos para atualizar a tela.

7. **O que e o RMI Registry?**  
   E um servico de nomes do RMI. Ele permite registrar um objeto remoto com um nome e depois localizar esse objeto a partir do cliente.

8. **Qual e a funcao de `LocateRegistry.getRegistry(...)`?**  
   Ela obtem uma referencia para o registry RMI que esta rodando em determinado host e porta.

9. **O que acontece no `registry.lookup("ServicoJogo21")`?**  
   O cliente procura no registry o objeto remoto registrado com o nome `ServicoJogo21` e recebe uma referencia para chamar seus metodos.

10. **Para que serve `UnicastRemoteObject`?**  
    Serve para exportar um objeto Java como objeto remoto RMI. No projeto, isso acontece no servidor e tambem no callback do cliente.

### Perguntas sobre a arquitetura do projeto

1. **Por que as regras do jogo ficam no servidor e nao no cliente?**  
   Para manter consistencia e evitar que cada cliente tenha sua propria versao do jogo. O servidor e a autoridade unica sobre cartas, turnos e resultados.

2. **Como o servidor impede que um jogador compre carta fora da sua vez?**  
   O servidor chama `validarVez(jogador)` antes de aceitar `pedirCarta` ou `pararJogada`. Se nao for a vez do jogador, ele lanca `RemoteException`.

3. **Como o sistema diferencia dois jogadores com o mesmo nome?**  
   O cliente cria um identificador interno juntando o nome escolhido com um trecho de UUID. Assim, nomes iguais continuam tendo ids diferentes.

4. **O que acontece se um jogador entra enquanto uma rodada ja esta em andamento?**  
   Ele e conectado, mas fica com status `AGUARDANDO_PROXIMA_RODADA`. Ele so participa da proxima rodada.

5. **O que acontece quando um jogador fecha a janela?**  
   O cliente chama `disconnectFromServer()`, que tenta executar `service.desconectar(playerName)`. O servidor remove o jogador da mesa.

6. **Por que os metodos principais do servidor sao `synchronized`?**  
   Para evitar conflitos quando varios clientes chamam o servidor ao mesmo tempo. Assim, o estado compartilhado da mesa e alterado de forma controlada.

7. **Como o servidor decide quando finalizar a rodada?**  
   Quando todos os jogadores da rodada ja jogaram, pararam ou estouraram, o servidor chama `finalizarRodada()`.

8. **Como o servidor inicia automaticamente a proxima rodada?**  
   Depois de finalizar uma rodada, ele chama `agendarProximaRodada()`, que cria uma thread, espera 5 segundos e inicia nova rodada se ainda houver jogadores.

9. **Por que o servidor envia apenas uma visao parcial das cartas dos outros jogadores?**  
   Para preservar a informacao do jogo. O jogador ve suas proprias cartas, mas nao recebe as cartas reais dos outros jogadores.

10. **Qual e a vantagem de centralizar o baralho no servidor?**  
    Garante que todos joguem com o mesmo baralho e impede que clientes manipulem cartas localmente.

### Perguntas sobre a interface grafica

1. **Por que o cliente usa `SwingWorker` nas chamadas remotas?**  
   Para executar chamadas RMI em segundo plano e evitar que a janela Swing trave enquanto aguarda resposta do servidor.

2. **O que poderia acontecer se uma chamada RMI fosse feita diretamente na thread da interface?**  
   A interface poderia congelar, parar de responder a cliques e parecer travada ate a chamada remota terminar.

3. **Como a interface sabe quando deve habilitar `Pedir carta` e `Parar`?**  
   O `Cliente` verifica se o status recebido em `EstadoPartida` e `EM_ANDAMENTO` e chama `gui.updateActionButtons(...)`.

4. **Qual e a funcao do metodo `renderState(...)`?**  
   Ele atualiza a tela com o estado recebido do servidor, redesenhando cartas, pontuacoes, status, fichas, aposta e historico.

5. **Como a interface mostra uma carta oculta do dealer?**  
   Enquanto `dealerRevelado` e falso, o servidor envia apenas a carta visivel. A GUI adiciona uma `CardView` oculta para representar a carta virada.

6. **Como a barra lateral diferencia o proprio jogador dos outros?**  
   Ela compara o id do jogador exibido com `currentPlayerId`. Se forem iguais, mostra `VOCE`, fundo diferente e borda dourada.

7. **Por que outros jogadores aparecem com cartas ocultas?**  
   Porque o servidor nao envia as cartas reais dos outros jogadores para o cliente. A GUI mostra cartas ocultas para indicar que eles participam da rodada.

8. **Qual e a funcao do `Timer` de 1500 ms?**  
   Atualizar periodicamente o estado local e a lista de jogadores, mantendo a tela sincronizada com o servidor.

9. **Como a interface exibe erros de comunicacao?**  
   O `Cliente` captura excecoes e chama `gui.showError(...)`, que escreve no log e mostra uma caixa de dialogo de erro.

10. **Como o resultado da rodada e exibido para o usuario?**  
    O servidor envia uma mensagem iniciada por `RESULTADO_RODADA|`. O callback do cliente extrai o resumo e chama `showRoundResultAnimation(...)`.

### Perguntas sobre callback

1. **O que e um callback em RMI?**  
   E um objeto remoto disponibilizado pelo cliente para que o servidor tambem consiga chamar metodos no cliente.

2. **Por que o callback e util neste projeto?**  
   Porque o servidor consegue avisar eventos importantes, como fim de rodada e mensagens da mesa, sem esperar o cliente fazer uma nova acao.

3. **Qual e a diferenca entre o cliente consultar o servidor e o servidor notificar o cliente?**  
   Consultar significa o cliente perguntar periodicamente. Notificar significa o servidor enviar uma mensagem quando algo acontece.

4. **Como o cliente disponibiliza um objeto remoto para o servidor chamar?**  
   Ele cria uma classe que implementa `ClienteCallback` e estende `UnicastRemoteObject`, depois envia esse objeto para o servidor no metodo `conectar`.

5. **Por que `GuiCallback` estende `UnicastRemoteObject`?**  
   Porque ele precisa ser exportado como objeto remoto para que o servidor consiga chamar `notificar(...)`.

6. **O que o servidor envia pela mensagem `RESULTADO_RODADA|...`?**  
   Envia um resumo textual da rodada, incluindo pontuacao do dealer, vencedores, empates, perdedores e possiveis empates entre jogadores.

7. **Por que o callback usa `SwingUtilities.invokeLater(...)` antes de atualizar a interface?**  
   Porque atualizacoes de Swing devem acontecer na thread da interface grafica. O callback pode ser executado por uma thread do RMI.

8. **O que poderia acontecer se o servidor tentasse notificar um cliente desconectado?**  
   A chamada poderia gerar `RemoteException`. No servidor, esse erro e capturado e registrado no console.

### Perguntas de defesa tecnica

1. **Quais dados trafegam do cliente para o servidor quando o jogador pede carta?**  
   Trafega principalmente o id do jogador em `service.pedirCarta(playerName)`.

2. **Quais dados trafegam do servidor para o cliente depois de uma jogada?**  
   O servidor retorna um `EstadoPartida` com cartas visiveis, pontuacao, saldo, aposta, historico e status.

3. **Como voce explicaria a diferenca entre `EstadoPartida` e `EstadoJogador`?**  
   `EstadoPartida` representa a visao completa do jogador local sobre sua partida. `EstadoJogador` e um resumo usado para montar a lista lateral de todos os jogadores.

4. **Por que o servidor nao envia as cartas reais dos outros jogadores?**  
   Para manter privacidade e coerencia com a regra visual da mesa. Cada cliente so deve conhecer suas cartas e informacoes publicas dos demais.

5. **Como o sistema mantem a consistencia entre varios clientes?**  
   Mantendo todo o estado no servidor e fazendo os clientes apenas chamarem metodos remotos e renderizarem o estado retornado.

6. **O que aconteceria se dois jogadores clicassem ao mesmo tempo?**  
   As chamadas chegariam ao servidor, mas os metodos `synchronized` fariam uma chamada ser processada por vez. Alem disso, `validarVez` garante que apenas o jogador da vez aja.

7. **Que parte do codigo garante a ordem dos turnos?**  
   A lista `ordemRodada`, o indice `turnoAtual`, e os metodos `prepararJogadorDaVez()` e `avancarTurnoOuFinalizar()`.

8. **Como o sistema trata um jogador que sai durante a rodada?**  
   O servidor remove o jogador, ajusta `ordemRodada` e `turnoAtual`, e chama `ajustarTurnoAposSaida()` ou finaliza a rodada se necessario.

9. **Quais melhorias seriam necessarias para muitos jogadores em uma rede real?**  
   Tratamento melhor de reconexao, timeout, logs estruturados, configuracao de host/porta, seguranca, autenticacao e talvez uma arquitetura mais escalavel que RMI puro.

10. **Como voce poderia testar a comunicacao RMI sem usar a interface grafica?**  
    Criando um cliente de console que usa `LocateRegistry`, faz `lookup("ServicoJogo21")`, registra um `ClienteCallbackImpl` e chama os metodos remotos diretamente.

## 18. Resumo para apresentacao

Uma forma simples de explicar:

> O servidor RMI e a autoridade da mesa. Ele guarda cartas, jogadores, turnos, apostas e resultados. O `Cliente` cuida da comunicacao RMI e chama os metodos remotos. O `ClienteGUI` cuida apenas da tela Swing. O RMI permite que o cliente chame o servidor, e o callback permite que o servidor envie avisos de volta ao cliente.

Em outras palavras:

- o servidor controla o jogo;
- `Cliente.java` controla o RMI;
- `ClienteGUI.java` controla a interface grafica;
- objetos serializaveis transportam estado;
- callback permite notificacoes assincronas;
- `SwingWorker` evita que a tela trave durante chamadas remotas.

