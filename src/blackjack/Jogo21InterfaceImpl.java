package blackjack;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servidor de uma mesa compartilhada de Blackjack.
 *
 * <p>Esta classe guarda o estado real do jogo. O cliente nunca altera cartas,
 * baralho, dealer ou turnos diretamente; ele apenas chama metodos remotos, e o
 * servidor decide se a acao e valida.</p>
 */
public class Jogo21InterfaceImpl extends UnicastRemoteObject implements Jogo21Interface {

    private static final long serialVersionUID = 1L;
    private static final int SALDO_INICIAL = 100;
    private static final int APOSTA_PADRAO = 10;

    /**
     * Representa um jogador conectado a mesa.
     *
     * <p>E uma classe interna porque esse estado so faz sentido dentro do
     * servidor. O cliente recebe apenas resumos serializaveis como
     * {@link EstadoJogador} e {@link EstadoPartida}.</p>
     */
    private static class Jogador {
        final String id;
        final String nome;
        final ClienteCallback callback;
        final List<Carta> mao = new ArrayList<>();
        int saldo = SALDO_INICIAL;
        int apostaAtual = 0;
        int vitorias = 0;
        int empates = 0;
        int derrotas = 0;
        EstadoPartida.Status status = EstadoPartida.Status.AGUARDANDO_PROXIMA_RODADA;
        boolean naRodadaAtual = false;

        Jogador(String id, String nome, ClienteCallback callback) {
            this.id = id;
            this.nome = nome;
            this.callback = callback;
        }

        /** Envia uma mensagem ao cliente deste jogador via callback RMI. */
        void notificar(String mensagem) {
            try {
                callback.notificar(mensagem);
            } catch (RemoteException e) {
                System.err.println("[SERVIDOR] Falha ao notificar " + nome + ": " + e.getMessage());
            }
        }
    }

    private final Map<String, Jogador> jogadores = new LinkedHashMap<>();
    private final List<String> ordemRodada = new ArrayList<>();
    private final List<Carta> maoDealer = new ArrayList<>();
    private List<Carta> baralho = new ArrayList<>();
    private int turnoAtual = -1;
    private boolean rodadaAtiva = false;
    private boolean dealerRevelado = false;
    private boolean exibindoResultado = false;

    public Jogo21InterfaceImpl() throws RemoteException {
        super();
    }

    /**
     * Adiciona um novo jogador a mesa.
     *
     * <p>Se nao existe rodada ativa, a entrada desse jogador inicia uma rodada.
     * Se a mesa ja esta em rodada ou exibindo resultado, o jogador entra apenas
     * na proxima rodada.</p>
     */
    @Override
    public synchronized EstadoPartida conectar(String idJogador, String nomeExibicao, ClienteCallback callback)
            throws RemoteException {
        if (idJogador == null || idJogador.isBlank()) {
            throw new RemoteException("Identificador do jogador nao pode ser vazio.");
        }
        if (nomeExibicao == null || nomeExibicao.isBlank()) {
            throw new RemoteException("Nome do jogador nao pode ser vazio.");
        }
        if (jogadores.containsKey(idJogador)) {
            throw new RemoteException("Jogador ja conectado.");
        }

        Jogador jogador = new Jogador(idJogador, nomeExibicao, callback);
        jogadores.put(idJogador, jogador);
        System.out.println("[SERVIDOR] Jogador entrou na mesa: " + nomeExibicao);

        if (!rodadaAtiva && !exibindoResultado) {
            iniciarNovaRodada();
            jogador.notificar("Voce entrou na mesa e esta na rodada atual.");
        } else {
            jogador.status = EstadoPartida.Status.AGUARDANDO_PROXIMA_RODADA;
            jogador.naRodadaAtual = false;
            jogador.notificar("Mesa em rodada. Voce entra na proxima rodada.");
            notificarJogadores(nomeExibicao + " entrou na mesa e aguardara a proxima rodada.");
        }

        return construirEstado(jogador);
    }

    /**
     * Compra uma carta para o jogador da vez.
     *
     * <p>Antes de comprar, o servidor chama {@link #validarVez(Jogador)} para
     * garantir que o jogador esta participando da rodada e realmente e o atual.
     * Se ele passar de 21, seu status muda para estouro e o turno avanca.</p>
     */
    @Override
    public synchronized EstadoPartida pedirCarta(String idJogador) throws RemoteException {
        Jogador jogador = obterJogador(idJogador);
        validarVez(jogador);

        Carta carta = comprarCarta();
        jogador.mao.add(carta);
        System.out.println("[SERVIDOR] " + jogador.nome + " recebeu: " + carta);
        jogador.notificar("Voce recebeu: " + carta);

        int pontuacao = calcularPontuacao(jogador.mao);
        if (pontuacao > 21) {
            jogador.status = EstadoPartida.Status.JOGADOR_ESTOUROU;
            jogador.notificar("Voce estourou com " + pontuacao + " pontos.");
            avancarTurnoOuFinalizar();
        }

        return construirEstado(jogador);
    }

    /**
     * Encerra a jogada do jogador atual sem comprar carta.
     *
     * <p>Depois que o jogador para, ele aguarda os demais. O servidor passa a vez
     * para o proximo jogador ou finaliza a rodada se todos ja jogaram.</p>
     */
    @Override
    public synchronized EstadoPartida pararJogada(String idJogador) throws RemoteException {
        Jogador jogador = obterJogador(idJogador);
        validarVez(jogador);

        jogador.status = EstadoPartida.Status.JOGADOR_PAROU;
        jogador.notificar("Voce parou. Aguarde os outros jogadores.");
        System.out.println("[SERVIDOR] " + jogador.nome + " parou.");
        avancarTurnoOuFinalizar();

        return construirEstado(jogador);
    }

    /** Retorna o estado atual da mesa visto por um jogador especifico. */
    @Override
    public synchronized EstadoPartida obterEstado(String idJogador) throws RemoteException {
        return construirEstado(obterJogador(idJogador));
    }

    /**
     * Retorna um resumo de todos os jogadores conectados.
     *
     * <p>Esse metodo e usado pela barra lateral da interface grafica.</p>
     */
    @Override
    public synchronized List<EstadoJogador> listarJogadores(String idJogador) throws RemoteException {
        obterJogador(idJogador);

        List<EstadoJogador> estados = new ArrayList<>();
        String idDaVez = idJogadorDaVez();
        for (Jogador jogador : jogadores.values()) {
            boolean proprioJogador = jogador.id.equals(idJogador);
            estados.add(new EstadoJogador(
                    jogador.id,
                    jogador.nome,
                    proprioJogador ? new ArrayList<>(jogador.mao) : List.of(),
                    proprioJogador ? calcularPontuacao(jogador.mao) : -1,
                    jogador.saldo,
                    jogador.apostaAtual,
                    jogador.vitorias,
                    jogador.empates,
                    jogador.derrotas,
                    statusVisivel(jogador),
                    jogador.naRodadaAtual,
                    jogador.id.equals(idDaVez)
            ));
        }
        return estados;
    }

    /**
     * Remove um jogador da mesa e ajusta a rodada se necessario.
     *
     * <p>Se o jogador removido estava antes do turno atual, o indice do turno e
     * reduzido para continuar apontando para a pessoa correta. Se a mesa fica
     * vazia, todo o estado e limpo.</p>
     */
    @Override
    public synchronized void desconectar(String idJogador) throws RemoteException {
        int indiceRemovido = ordemRodada.indexOf(idJogador);
        Jogador removido = jogadores.remove(idJogador);
        if (removido == null) {
            return;
        }

        ordemRodada.remove(idJogador);
        System.out.println("[SERVIDOR] Jogador saiu da mesa: " + removido.nome);
        notificarJogadores(removido.nome + " saiu da mesa.");

        if (jogadores.isEmpty()) {
            limparMesa();
            return;
        }

        if (rodadaAtiva) {
            if (indiceRemovido >= 0 && indiceRemovido < turnoAtual) {
                turnoAtual--;
            }
            if (ordemRodada.isEmpty()) {
                finalizarRodada();
            } else {
                ajustarTurnoAposSaida();
            }
        }
    }

    /**
     * Inicia uma rodada para todos os jogadores presentes na mesa.
     *
     * <p>Jogadores que entram depois desse momento ficam aguardando a proxima.
     * Aqui o servidor limpa maos antigas, cria um novo baralho, distribui duas
     * cartas para cada jogador e duas para o dealer.</p>
     */
    private void iniciarNovaRodada() {
        ordemRodada.clear();
        maoDealer.clear();
        baralho = gerarBaralhoEmbaralhado();
        dealerRevelado = false;
        exibindoResultado = false;
        rodadaAtiva = true;
        turnoAtual = 0;

        for (Jogador jogador : jogadores.values()) {
            jogador.mao.clear();
            prepararAposta(jogador);
            jogador.naRodadaAtual = true;
            jogador.status = EstadoPartida.Status.AGUARDANDO_VEZ;
            ordemRodada.add(jogador.id);
        }

        for (int i = 0; i < 2; i++) {
            for (String id : ordemRodada) {
                jogadores.get(id).mao.add(comprarCarta());
            }
            maoDealer.add(comprarCarta());
        }

        prepararJogadorDaVez();
        notificarJogadores("Nova rodada iniciada com " + ordemRodada.size() + " jogador(es).");
    }

    /**
     * Marca quem e o jogador da vez.
     *
     * <p>Somente o jogador com status {@code EM_ANDAMENTO} pode pedir carta ou
     * parar. Os demais ficam em {@code AGUARDANDO_VEZ}.</p>
     */
    private void prepararJogadorDaVez() {
        String idDaVez = idJogadorDaVez();
        for (String id : ordemRodada) {
            Jogador jogador = jogadores.get(id);
            if (jogador == null || jogador.status != EstadoPartida.Status.AGUARDANDO_VEZ && jogador.status != EstadoPartida.Status.EM_ANDAMENTO) {
                continue;
            }
            jogador.status = id.equals(idDaVez) ? EstadoPartida.Status.EM_ANDAMENTO : EstadoPartida.Status.AGUARDANDO_VEZ;
        }

        Jogador jogadorDaVez = idDaVez == null ? null : jogadores.get(idDaVez);
        if (jogadorDaVez != null) {
            jogadorDaVez.notificar("Sua vez de jogar.");
        }
    }

    /**
     * Move o turno para o proximo jogador apto ou finaliza a rodada.
     */
    private void avancarTurnoOuFinalizar() {
        while (turnoAtual + 1 < ordemRodada.size()) {
            turnoAtual++;
            Jogador proximo = jogadores.get(ordemRodada.get(turnoAtual));
            if (proximo != null && proximo.status == EstadoPartida.Status.AGUARDANDO_VEZ) {
                prepararJogadorDaVez();
                return;
            }
        }

        finalizarRodada();
    }

    /**
     * Recalcula o turno quando alguem sai durante uma rodada.
     */
    private void ajustarTurnoAposSaida() {
        if (turnoAtual < 0) {
            turnoAtual = 0;
        }

        while (turnoAtual < ordemRodada.size()) {
            Jogador jogador = jogadores.get(ordemRodada.get(turnoAtual));
            if (jogador != null && (jogador.status == EstadoPartida.Status.AGUARDANDO_VEZ
                    || jogador.status == EstadoPartida.Status.EM_ANDAMENTO)) {
                prepararJogadorDaVez();
                return;
            }
            turnoAtual++;
        }

        finalizarRodada();
    }

    /**
     * Finaliza a rodada, revela o dealer e calcula o resultado de cada jogador.
     *
     * <p>Depois de calcular o resultado, o servidor notifica os clientes e deixa
     * a mesa parada por 5 segundos para que todos vejam as cartas reveladas.</p>
     */
    private void finalizarRodada() {
        dealerRevelado = true;
        exibindoResultado = true;

        boolean existeJogadorValido = false;
        for (String id : ordemRodada) {
            Jogador jogador = jogadores.get(id);
            if (jogador != null && calcularPontuacao(jogador.mao) <= 21) {
                existeJogadorValido = true;
                break;
            }
        }

        if (existeJogadorValido) {
            while (calcularPontuacao(maoDealer) < 17) {
                maoDealer.add(comprarCarta());
            }
        }

        int pontDealer = calcularPontuacao(maoDealer);
        String resumoRodada = montarResumoRodada(pontDealer);
        for (String id : ordemRodada) {
            Jogador jogador = jogadores.get(id);
            if (jogador == null) {
                continue;
            }

            int pontJogador = calcularPontuacao(jogador.mao);
            if (pontJogador > 21) {
                jogador.status = EstadoPartida.Status.JOGADOR_ESTOUROU;
            } else if (pontDealer > 21) {
                jogador.status = EstadoPartida.Status.DEALER_ESTOUROU;
            } else if (pontJogador > pontDealer) {
                jogador.status = EstadoPartida.Status.JOGADOR_VENCEU;
            } else {
                jogador.status = EstadoPartida.Status.DEALER_VENCEU;
            }
            registrarResultadoFinanceiro(jogador);
            jogador.notificar(mensagemResultado(jogador.status, pontJogador, pontDealer));
        }
        notificarJogadores("RESULTADO_RODADA|" + resumoRodada);

        rodadaAtiva = false;
        turnoAtual = -1;
        notificarJogadores("Rodada encerrada.");
        agendarProximaRodada();
    }

    /**
     * Monta o objeto de estado enviado para um jogador.
     */
    private EstadoPartida construirEstado(Jogador jogador) {
        EstadoPartida.Status status = statusVisivel(jogador);
        return new EstadoPartida(
                new ArrayList<>(jogador.mao),
                cartasVisiveisDealer(),
                dealerRevelado,
                calcularPontuacao(jogador.mao),
                dealerRevelado ? calcularPontuacao(maoDealer) : -1,
                jogador.saldo,
                jogador.apostaAtual,
                jogador.vitorias,
                jogador.empates,
                jogador.derrotas,
                status
        );
    }

    /**
     * Reserva a aposta ficticia do jogador para a rodada atual.
     */
    private void prepararAposta(Jogador jogador) {
        if (jogador.saldo <= 0) {
            jogador.saldo = SALDO_INICIAL;
            jogador.notificar("Seu saldo foi recarregado com " + SALDO_INICIAL + " fichas ficticias.");
        }

        jogador.apostaAtual = Math.min(APOSTA_PADRAO, jogador.saldo);
        jogador.saldo -= jogador.apostaAtual;
        jogador.notificar("Aposta automatica de " + jogador.apostaAtual + " fichas registrada.");
    }

    /**
     * Atualiza saldo e historico depois que o resultado da rodada e conhecido.
     */
    private void registrarResultadoFinanceiro(Jogador jogador) {
        if (jogador.status == EstadoPartida.Status.JOGADOR_VENCEU || jogador.status == EstadoPartida.Status.DEALER_ESTOUROU) {
            jogador.vitorias++;
            jogador.saldo += jogador.apostaAtual * 2;
        } else if (jogador.status == EstadoPartida.Status.EMPATE) {
            jogador.empates++;
        } else if (jogador.status == EstadoPartida.Status.JOGADOR_ESTOUROU || jogador.status == EstadoPartida.Status.DEALER_VENCEU) {
            jogador.derrotas++;
        }
    }

    /**
     * Retorna apenas as cartas do dealer que podem ser enviadas ao cliente.
     */
    private List<Carta> cartasVisiveisDealer() {
        if (dealerRevelado) {
            return new ArrayList<>(maoDealer);
        }
        if (maoDealer.isEmpty()) {
            return List.of();
        }
        return List.of(maoDealer.get(0));
    }

    /**
     * Converte o estado interno do jogador em um status visivel para o cliente.
     */
    private EstadoPartida.Status statusVisivel(Jogador jogador) {
        if (!jogador.naRodadaAtual) {
            return EstadoPartida.Status.AGUARDANDO_PROXIMA_RODADA;
        }
        return jogador.status;
    }

    /**
     * Garante que o jogador pode executar uma acao de turno.
     */
    private void validarVez(Jogador jogador) throws RemoteException {
        if (!rodadaAtiva || !jogador.naRodadaAtual) {
            throw new RemoteException("Voce esta aguardando a proxima rodada.");
        }
        if (!jogador.id.equals(idJogadorDaVez()) || jogador.status != EstadoPartida.Status.EM_ANDAMENTO) {
            throw new RemoteException("Ainda nao e sua vez.");
        }
    }

    /** Busca um jogador pelo id interno. */
    private Jogador obterJogador(String idJogador) throws RemoteException {
        Jogador jogador = jogadores.get(idJogador);
        if (jogador == null) {
            throw new RemoteException("Jogador nao encontrado na mesa.");
        }
        return jogador;
    }

    /** Retorna o id do jogador da vez ou null se nao houver rodada ativa. */
    private String idJogadorDaVez() {
        if (!rodadaAtiva || turnoAtual < 0 || turnoAtual >= ordemRodada.size()) {
            return null;
        }
        return ordemRodada.get(turnoAtual);
    }

    /**
     * Compra uma carta do baralho compartilhado.
     *
     * <p>Se o baralho esta ficando baixo, um novo baralho e adicionado. Em uma
     * versao futura, isso poderia ser trocado por um sapato com varios baralhos.</p>
     */
    private Carta comprarCarta() {
        if (baralho.size() < 10) {
            baralho.addAll(gerarBaralhoEmbaralhado());
        }
        return baralho.remove(baralho.size() - 1);
    }

    /** Limpa todo o estado da mesa quando nao ha jogadores conectados. */
    private void limparMesa() {
        ordemRodada.clear();
        maoDealer.clear();
        baralho.clear();
        rodadaAtiva = false;
        dealerRevelado = false;
        exibindoResultado = false;
        turnoAtual = -1;
    }

    /**
     * Agenda a proxima rodada depois do intervalo de exibicao do resultado.
     */
    private void agendarProximaRodada() {
        Thread proximaRodada = new Thread(() -> {
            try {
                Thread.sleep(1000); // Aguarda 1 segundo para que todos vejam o resultado da rodada
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            synchronized (Jogo21InterfaceImpl.this) {
                if (!jogadores.isEmpty() && exibindoResultado) {
                    iniciarNovaRodada();
                }
            }
        }, "proxima-rodada-blackjack");
        proximaRodada.setDaemon(true);
        proximaRodada.start();
    }

    /** Envia uma mensagem para todos os jogadores conectados. */
    private void notificarJogadores(String mensagem) {
        for (Jogador jogador : jogadores.values()) {
            jogador.notificar(mensagem);
        }
    }

    /** Cria uma mensagem individual de resultado contra o dealer. */
    private String mensagemResultado(EstadoPartida.Status status, int pontJogador, int pontDealer) {
        return switch (status) {
            case JOGADOR_ESTOUROU -> "Voce estourou com " + pontJogador + " pontos. Dealer venceu.";
            case DEALER_ESTOUROU -> "Dealer estourou com " + pontDealer + " pontos. Voce venceu a aposta!";
            case JOGADOR_VENCEU -> "Voce venceu com " + pontJogador + " contra " + pontDealer + " e ganhou a aposta.";
            case EMPATE -> "Empate entre jogadores com " + pontJogador + " pontos.";
            case DEALER_VENCEU -> "Dealer venceu com " + pontDealer + " contra " + pontJogador + ". Aposta perdida.";
            default -> "";
        };
    }

    /**
     * Cria o resumo geral exibido no fim da rodada.
     */
    private String montarResumoRodada(int pontDealer) {
        List<String> vencedores = new ArrayList<>();
        List<String> perdedores = new ArrayList<>();

        for (String id : ordemRodada) {
            Jogador jogador = jogadores.get(id);
            if (jogador == null) {
                continue;
            }

            int pontos = calcularPontuacao(jogador.mao);
            boolean venceuDealer = pontos <= 21 && (pontDealer > 21 || pontos > pontDealer);
            if (venceuDealer) {
                vencedores.add(jogador.nome + " (" + pontos + ", +" + jogador.apostaAtual + " fichas)");
            } else {
                perdedores.add(jogador.nome + " (" + pontos + ", -" + jogador.apostaAtual + " fichas)");
            }
        }

        return "Dealer: " + pontDealer  + "\nVencedores: " + textoLista(vencedores)  + "\nPerdedores: " + textoLista(perdedores);
    }

    /** Formata listas do resumo final, evitando lista vazia sem texto. */
    private String textoLista(List<String> valores) {
        return valores.isEmpty() ? "nenhum" : String.join(", ", valores);
    }

    /**
     * Calcula a pontuacao de uma mao de Blackjack.
     *
     * <p>O As entra inicialmente como 11. Se a mao passar de 21, cada As pode
     * ser convertido para 1 subtraindo 10 pontos do total.</p>
     */
    static int calcularPontuacao(List<Carta> mao) {
        int total = 0;
        int ases = 0;

        for (Carta c : mao) {
            total += c.getPontos();
            if (c.isAs()) {
                ases++;
            }
        }

        while (total > 21 && ases > 0) {
            total -= 10;
            ases--;
        }

        return total;
    }

    /**
     * Gera um baralho tradicional de 52 cartas e embaralha.
     */
    private static List<Carta> gerarBaralhoEmbaralhado() {
        List<Carta> novoBaralho = new ArrayList<>(52);
        for (Carta.Nipe nipe : Carta.Nipe.values()) {
            for (Carta.Valor valor : Carta.Valor.values()) {
                novoBaralho.add(new Carta(nipe, valor));
            }
        }
        Collections.shuffle(novoBaralho);
        return novoBaralho;
    }
}
