package blackjack;

import java.io.Serializable;
import java.util.List;

/**
 * Resumo serializavel de um jogador para exibicao da mesa compartilhada.
 *
 * <p>O servidor envia varios objetos desse tipo para o cliente montar a lista
 * lateral de jogadores. Essa classe nao executa regras do jogo; ela apenas
 * transporta dados prontos para exibicao.</p>
 */
public class EstadoJogador implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String nome;
    private final List<Carta> cartas;
    private final int pontuacao;
    private final EstadoPartida.Status status;
    private final boolean naRodadaAtual;
    private final boolean vezAtual;

    public EstadoJogador(
            String id,
            String nome,
            List<Carta> cartas,
            int pontuacao,
            EstadoPartida.Status status,
            boolean naRodadaAtual,
            boolean vezAtual) {
        this.id = id;
        this.nome = nome;
        this.cartas = cartas;
        this.pontuacao = pontuacao;
        this.status = status;
        this.naRodadaAtual = naRodadaAtual;
        this.vezAtual = vezAtual;
    }

    /**
     * Identificador interno do jogador.
     *
     * <p>E diferente do nome de exibicao para evitar conflito quando duas
     * pessoas escolhem o mesmo nome.</p>
     */
    public String getId() {
        return id;
    }

    /** Nome mostrado na interface grafica. */
    public String getNome() {
        return nome;
    }

    /** Cartas atualmente visiveis na mao do jogador. */
    public List<Carta> getCartas() {
        return cartas;
    }

    /** Pontuacao calculada pelo servidor para essa mao. */
    public int getPontuacao() {
        return pontuacao;
    }

    /** Situacao do jogador na rodada atual. */
    public EstadoPartida.Status getStatus() {
        return status;
    }

    /** Indica se o jogador participa da rodada em andamento. */
    public boolean isNaRodadaAtual() {
        return naRodadaAtual;
    }

    /** Indica se esse jogador e o proximo a jogar. */
    public boolean isVezAtual() {
        return vezAtual;
    }
}
