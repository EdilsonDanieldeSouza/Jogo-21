package blackjack;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação da lógica do servidor de Blackjack.
 *
 * Gerencia partidas isoladas por jogador (thread-safe via ConcurrentHashMap),
 * o baralho, o turno automático do dealer e o cálculo dinâmico de pontuação
 * com tratamento correto do Ás (1 ou 11).
 */
public class Jogo21InterfaceImpl extends UnicastRemoteObject implements Jogo21Interface {

    private static final long serialVersionUID = 1L;

    // ─── Estado interno de uma partida individual ─────────────────────────────

    /**
     * Encapsula todos os dados de uma partida de um único jogador.
     * O isolamento é garantido pois cada jogador possui sua própria instância.
     */
    private static class Partida {
        final String nomeJogador;
        final ClienteCallback callback;
        final List<Carta> maoPropria  = new ArrayList<>();
        final List<Carta> maoDealer   = new ArrayList<>();
        final List<Carta> baralho;
        boolean encerrada = false;

        Partida(String nome, ClienteCallback callback) {
            this.nomeJogador = nome;
            this.callback    = callback;
            this.baralho     = gerarBaralhoEmbaralhado();
        }

        /** Retira e retorna a próxima carta do topo do baralho. */
        Carta comprarCarta() {
            return baralho.remove(baralho.size() - 1);
        }

        /** Envia uma notificação ao cliente via callback (ignora falhas silenciosamente). */
        void notificar(String msg) {
            try {
                callback.notificar(msg);
            } catch (RemoteException e) {
                System.err.println("[SERVIDOR] Falha ao notificar " + nomeJogador + ": " + e.getMessage());
            }
        }
    }


    private final Map<String, Partida> partidas = new ConcurrentHashMap<>();

    public Jogo21InterfaceImpl() throws RemoteException {
        super();
    }

    @Override
    public synchronized EstadoPartida conectar(String nome, ClienteCallback callback) throws RemoteException {
        System.out.println("[SERVIDOR] Jogador conectado: " + nome);

        // Cria (ou reinicia) a partida para este jogador
        Partida p = new Partida(nome, callback);
        partidas.put(nome, p);

        // Distribui 2 cartas para o jogador e 2 para o dealer
        p.maoPropria.add(p.comprarCarta());
        p.maoPropria.add(p.comprarCarta());
        p.maoDealer.add(p.comprarCarta());
        p.maoDealer.add(p.comprarCarta());

        p.notificar("Partida iniciada! Boa sorte, " + nome + "!");

        return construirEstado(p, false);
    }

    @Override
    public synchronized EstadoPartida pedirCarta(String nome) throws RemoteException {
        Partida p = obterPartidaAtiva(nome);

        Carta novaCarta = p.comprarCarta();
        p.maoPropria.add(novaCarta);
        System.out.println("[SERVIDOR] " + nome + " recebeu: " + novaCarta);
        p.notificar("Você recebeu: " + novaCarta);

        int pontuacao = calcularPontuacao(p.maoPropria);

        if (pontuacao > 21) {
            // Jogador estourou — encerra imediatamente
            p.encerrada = true;
            p.notificar("Você estourou com " + pontuacao + " pontos! Dealer vence.");
            System.out.println("[SERVIDOR] " + nome + " estourou (" + pontuacao + " pts).");
            return construirEstadoFinal(p, EstadoPartida.Status.JOGADOR_ESTOUROU);
        }

        return construirEstado(p, false);
    }

    @Override
    public synchronized EstadoPartida pararJogada(String nome) throws RemoteException {
        Partida p = obterPartidaAtiva(nome);

        p.notificar("Você parou. Vez do dealer...");
        System.out.println("[SERVIDOR] " + nome + " parou. Executando turno do dealer.");

        // Turno automático do dealer: compra até atingir >= 17
        executarTurnoDealer(p);

        int pontJogador = calcularPontuacao(p.maoPropria);
        int pontDealer  = calcularPontuacao(p.maoDealer);

        System.out.println("[SERVIDOR] Resultado — " + nome + ": " + pontJogador
                + " pts | Dealer: " + pontDealer + " pts");

        // Determina resultado conforme as regras
        EstadoPartida.Status resultado;
        if (pontDealer > 21) {
            resultado = EstadoPartida.Status.DEALER_ESTOUROU;
            p.notificar("Dealer estourou! Você venceu!");
        } else if (pontJogador > pontDealer) {
            resultado = EstadoPartida.Status.JOGADOR_VENCEU;
            p.notificar("Você venceu com " + pontJogador + " contra " + pontDealer + " do dealer!");
        } else {
            // Empate também resulta em vitória do dealer
            resultado = EstadoPartida.Status.DEALER_VENCEU;
            p.notificar("Dealer venceu com " + pontDealer + " pontos.");
        }

        p.encerrada = true;
        return construirEstadoFinal(p, resultado);
    }

    
    private void executarTurnoDealer(Partida p) {
        while (calcularPontuacao(p.maoDealer) < 17) {
            Carta c = p.comprarCarta();
            p.maoDealer.add(c);
            System.out.println("[SERVIDOR] Dealer comprou: " + c
                    + " | Total: " + calcularPontuacao(p.maoDealer));
            p.notificar("Dealer comprou: " + c
                    + " [" + calcularPontuacao(p.maoDealer) + " pts]");
        }
    }

   
    static int calcularPontuacao(List<Carta> mao) {
        int total  = 0;
        int ases   = 0;

        for (Carta c : mao) {
            total += c.getPontos();
            if (c.isAs()) ases++;
        }

        // Ajusta Ases de 11 → 1 enquanto estourar
        while (total > 21 && ases > 0) {
            total -= 10; // transforma 11 em 1
            ases--;
        }

        return total;
    }

    /**
     * Constrói o estado da partida durante o turno do jogador
     * (carta oculta do dealer não revelada).
     */
    private EstadoPartida construirEstado(Partida p, boolean revelarDealer) {
        return new EstadoPartida(
                new ArrayList<>(p.maoPropria),
                new ArrayList<>(p.maoDealer),
                revelarDealer,
                calcularPontuacao(p.maoPropria),
                revelarDealer ? calcularPontuacao(p.maoDealer) : -1,
                EstadoPartida.Status.EM_ANDAMENTO
        );
    }

    /**
     * Constrói o estado final da partida com dealer completamente revelado.
     */
    private EstadoPartida construirEstadoFinal(Partida p, EstadoPartida.Status status) {
        return new EstadoPartida(
                new ArrayList<>(p.maoPropria),
                new ArrayList<>(p.maoDealer),
                true,
                calcularPontuacao(p.maoPropria),
                calcularPontuacao(p.maoDealer),
                status
        );
    }

    /**
     * Recupera a partida ativa de um jogador, lançando exceção se não existir
     * ou se já estiver encerrada.
     */
    private Partida obterPartidaAtiva(String nome) throws RemoteException {
        Partida p = partidas.get(nome);
        if (p == null) {
            throw new RemoteException("Jogador '" + nome + "' não encontrado. Use conectar() primeiro.");
        }
        if (p.encerrada) {
            throw new RemoteException("A partida de '" + nome + "' já foi encerrada.");
        }
        return p;
    }

   
    /**
     * Gera um baralho completo de 52 cartas (4 naipes × 13 valores) embaralhado.
     */
    private static List<Carta> gerarBaralhoEmbaralhado() {
        List<Carta> baralho = new ArrayList<>(52);
        for (Carta.Nipe nipe : Carta.Nipe.values()) {
            for (Carta.Valor valor : Carta.Valor.values()) {
                baralho.add(new Carta(nipe, valor));
            }
        }
        Collections.shuffle(baralho);
        return baralho;
    }
}
