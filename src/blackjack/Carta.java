package blackjack;

import java.io.Serializable;

/**
 * Representa uma carta do baralho.
 *
 * <p>Implementa {@link Serializable} porque objetos desse tipo sao enviados
 * entre servidor e cliente via RMI. Em RMI, objetos serializaveis sao
 * transmitidos por valor, ou seja, o cliente recebe uma copia da carta.</p>
 */
public class Carta implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Naipes disponiveis em um baralho tradicional.
     *
     * <p>Cada naipe possui um simbolo textual usado na exibicao das cartas.</p>
     */
    public enum Nipe {
        COPAS("\u2665"),
        OUROS("\u2666"),
        PAUS("\u2663"),
        ESPADAS("\u2660");

        private final String simbolo;

        Nipe(String simbolo) {
            this.simbolo = simbolo;
        }

        /** Retorna o simbolo visual do naipe. */
        public String getSimbolo() {
            return simbolo;
        }
    }

    /**
     * Valores possiveis das cartas.
     *
     * <p>As figuras valem 10 pontos. O As comeca valendo 11, mas a regra para
     * transforma-lo em 1 quando a mao estoura fica no servidor, em
     * {@code calcularPontuacao}.</p>
     */
    public enum Valor {
        DOIS("2", 2),
        TRES("3", 3),
        QUATRO("4", 4),
        CINCO("5", 5),
        SEIS("6", 6),
        SETE("7", 7),
        OITO("8", 8),
        NOVE("9", 9),
        DEZ("10", 10),
        VALETE("J", 10),
        DAMA("Q", 10),
        REI("K", 10),
        AS("A", 11);

        private final String nome;
        private final int pontos;

        Valor(String nome, int pontos) {
            this.nome = nome;
            this.pontos = pontos;
        }

        /** Nome curto usado na interface, como A, K, Q ou 10. */
        public String getNome() {
            return nome;
        }

        /** Pontos base do valor da carta. */
        public int getPontos() {
            return pontos;
        }
    }

    private final Nipe nipe;
    private final Valor valor;

    /**
     * Cria uma carta com um naipe e um valor.
     *
     * @param nipe naipe da carta
     * @param valor valor da carta
     */
    public Carta(Nipe nipe, Valor valor) {
        this.nipe = nipe;
        this.valor = valor;
    }

    /** Retorna o naipe da carta. */
    public Nipe getNipe() {
        return nipe;
    }

    /** Retorna o valor da carta. */
    public Valor getValor() {
        return valor;
    }

    /** Retorna a pontuacao base da carta. */
    public int getPontos() {
        return valor.getPontos();
    }

    /** Indica se a carta e um As. */
    public boolean isAs() {
        return valor == Valor.AS;
    }

    /** Retorna uma representacao curta, como A de copas ou 10 de paus. */
    @Override
    public String toString() {
        return valor.getNome() + nipe.getSimbolo();
    }
}
