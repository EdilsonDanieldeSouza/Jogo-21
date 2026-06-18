package blackjack;

import java.io.Serializable;

/**
 * Representa uma carta do baralho.
 * Implementa Serializable para ser transmitida por valor via RMI.
 */
public class Carta implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Nipe {
        COPAS("♥"), OUROS("♦"), PAUS("♣"), ESPADAS("♠");

        private final String simbolo;

        Nipe(String simbolo) {
            this.simbolo = simbolo;
        }

        public String getSimbolo() {
            return simbolo;
        }
    }

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
        AS("A", 11); // Ás começa valendo 11

        private final String nome;
        private final int pontos;

        Valor(String nome, int pontos) {
            this.nome = nome;
            this.pontos = pontos;
        }

        public String getNome() {
            return nome;
        }

        public int getPontos() {
            return pontos;
        }
    }

    private final Nipe nipe;
    private final Valor valor;

    public Carta(Nipe nipe, Valor valor) {
        this.nipe = nipe;
        this.valor = valor;
    }

    public Nipe getNipe() {
        return nipe;
    }

    public Valor getValor() {
        return valor;
    }

    public int getPontos() {
        return valor.getPontos();
    }

    public boolean isAs() {
        return valor == Valor.AS;
    }

    @Override
    public String toString() {
        return valor.getNome() + nipe.getSimbolo();
    }
}
