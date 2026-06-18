package blackjack;

import java.io.Serializable;
import java.util.List;

/**
 * Objeto serializável que representa o estado atual da partida.
 * É transmitido por valor (cópia) do servidor para o cliente via RMI.
 */
public class EstadoPartida implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        /** Partida em andamento — aguardando ação do jogador. */
        EM_ANDAMENTO,
        /** Jogador estourou (> 21). Dealer vence. */
        JOGADOR_ESTOUROU,
        /** Dealer estourou (> 21). Jogador vence. */
        DEALER_ESTOUROU,
        /** Jogador venceu por maior pontuação. */
        JOGADOR_VENCEU,
        /** Dealer venceu (empate ou maior pontuação). */
        DEALER_VENCEU
    }

    private final List<Carta> cartasJogador;
    private final List<Carta> cartasDealer;

    /**
     * Indica se a carta oculta do dealer deve ser exibida.
     * Durante a vez do jogador, apenas a primeira carta do dealer é visível.
     * Após o Stand, todas as cartas são reveladas.
     */
    private final boolean dealerRevelado;

    private final int pontuacaoJogador;
    private final int pontuacaoDealer;   // -1 quando dealer não revelado
    private final Status status;

    public EstadoPartida(
            List<Carta> cartasJogador,
            List<Carta> cartasDealer,
            boolean dealerRevelado,
            int pontuacaoJogador,
            int pontuacaoDealer,
            Status status) {
        this.cartasJogador = cartasJogador;
        this.cartasDealer = cartasDealer;
        this.dealerRevelado = dealerRevelado;
        this.pontuacaoJogador = pontuacaoJogador;
        this.pontuacaoDealer = pontuacaoDealer;
        this.status = status;
    }

    public List<Carta> getCartasJogador() {
        return cartasJogador;
    }

    public List<Carta> getCartasDealer() {
        return cartasDealer;
    }

    public boolean isDealerRevelado() {
        return dealerRevelado;
    }

    public int getPontuacaoJogador() {
        return pontuacaoJogador;
    }

    public int getPontuacaoDealer() {
        return pontuacaoDealer;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isPartidaEncerrada() {
        return status != Status.EM_ANDAMENTO;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    private String formatarMao(List<Carta> mao) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mao.size(); i++) {
            if (i > 0) sb.append("  ");
            sb.append(mao.get(i));
        }
        return sb.toString();
    }

    private String descricaoStatus() {
        return switch (status) {
            case JOGADOR_ESTOUROU -> "Você estourou! Dealer vence.";
            case DEALER_ESTOUROU  -> "Dealer estourou! Você vence!";
            case JOGADOR_VENCEU   -> "Você venceu!";
            case DEALER_VENCEU    -> "Dealer venceu.";
            default               -> "";
        };
    }
}
