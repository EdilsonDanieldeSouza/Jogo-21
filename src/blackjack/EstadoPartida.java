package blackjack;

import java.io.Serializable;
import java.util.List;

/**
 * Estado serializavel da mesa visto por um jogador especifico.
 *
 * <p>O servidor envia este objeto para o cliente sempre que a tela precisa ser
 * atualizada. Ele nao contem regra de negocio; apenas representa o estado ja
 * calculado pelo servidor.</p>
 */
public class EstadoPartida implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        /** E a vez do jogador tomar uma decisao. */
        EM_ANDAMENTO,
        /** Jogador passou de 21 pontos. */
        JOGADOR_ESTOUROU,
        /** Dealer passou de 21 pontos. */
        DEALER_ESTOUROU,
        /** Jogador venceu o dealer por pontuacao. */
        JOGADOR_VENCEU,
        /** Dealer venceu por pontuacao maior ou empate. */
        DEALER_VENCEU,
        /** Jogador entrou durante uma rodada e so jogara na proxima. */
        AGUARDANDO_PROXIMA_RODADA,
        /** Jogador esta na rodada, mas ainda nao e sua vez. */
        AGUARDANDO_VEZ,
        /** Jogador escolheu parar e aguarda os demais. */
        JOGADOR_PAROU
    }

    private final List<Carta> cartasJogador;
    private final List<Carta> cartasDealer;
    private final boolean dealerRevelado;
    private final int pontuacaoJogador;
    private final int pontuacaoDealer;
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

    /** Cartas do dealer. Antes do fim da rodada, uma delas fica oculta na tela. */
    public List<Carta> getCartasDealer() {
        return cartasDealer;
    }

    /** Define se todas as cartas do dealer podem ser mostradas. */
    public boolean isDealerRevelado() {
        return dealerRevelado;
    }

    /** Pontuacao do jogador local. */
    public int getPontuacaoJogador() {
        return pontuacaoJogador;
    }

    /** Pontuacao do dealer. Vale -1 enquanto o dealer nao foi revelado. */
    public int getPontuacaoDealer() {
        return pontuacaoDealer;
    }

    /** Status atual do jogador local na mesa. */
    public Status getStatus() {
        return status;
    }

    /** Retorna true quando a rodada ja tem um resultado final para o jogador. */
    public boolean isPartidaEncerrada() {
        return status == Status.JOGADOR_ESTOUROU
                || status == Status.DEALER_ESTOUROU
                || status == Status.JOGADOR_VENCEU
                || status == Status.DEALER_VENCEU;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Estado da partida ===").append(System.lineSeparator());
        sb.append("Jogador: ")
                .append(formatarMao(cartasJogador))
                .append(" [")
                .append(pontuacaoJogador)
                .append(" pts]")
                .append(System.lineSeparator());

        sb.append("Dealer: ");
        if (dealerRevelado) {
            sb.append(formatarMao(cartasDealer))
                    .append(" [")
                    .append(pontuacaoDealer)
                    .append(" pts]");
        } else if (cartasDealer.isEmpty()) {
            sb.append("(sem cartas)");
        } else {
            sb.append(cartasDealer.get(0)).append("  [carta oculta]");
        }

        String descricao = descricaoStatus();
        if (!descricao.isEmpty()) {
            sb.append(System.lineSeparator()).append(descricao);
        }
        return sb.toString();
    }

    private String formatarMao(List<Carta> mao) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mao.size(); i++) {
            if (i > 0) {
                sb.append("  ");
            }
            sb.append(mao.get(i));
        }
        return sb.toString();
    }

    private String descricaoStatus() {
        return switch (status) {
            case JOGADOR_ESTOUROU -> "Voce estourou! Dealer vence.";
            case DEALER_ESTOUROU -> "Dealer estourou! Voce vence!";
            case JOGADOR_VENCEU -> "Voce venceu!";
            case DEALER_VENCEU -> "Dealer venceu.";
            case AGUARDANDO_PROXIMA_RODADA -> "Aguardando a proxima rodada.";
            case AGUARDANDO_VEZ -> "Aguardando sua vez.";
            case JOGADOR_PAROU -> "Voce parou. Aguarde o resultado.";
            default -> "";
        };
    }
}
