package blackjack;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Contrato remoto do jogo de Blackjack.
 *
 * <p>Esta interface define tudo que um cliente pode pedir ao servidor.
 * Como ela estende {@link Remote}, seus metodos podem ser chamados por RMI.
 * Por isso, todos os metodos declaram {@link RemoteException}.</p>
 */
public interface Jogo21Interface extends Remote {

    /**
     * Conecta um jogador a mesa.
     *
     * <p>O id e usado internamente para diferenciar jogadores, mesmo quando
     * duas pessoas escolhem o mesmo nome. O nome de exibicao e o texto que
     * aparece na interface grafica. O callback permite que o servidor envie
     * avisos para o cliente.</p>
     *
     * @param idJogador identificador unico criado pelo cliente
     * @param nomeExibicao nome que sera mostrado aos jogadores
     * @param callback objeto remoto do cliente para receber notificacoes
     * @return estado inicial da mesa para esse jogador
     * @throws RemoteException se houver falha de comunicacao ou dados invalidos
     */
    EstadoPartida conectar(String idJogador, String nomeExibicao, ClienteCallback callback) throws RemoteException;

    /**
     * Compra uma carta para o jogador da vez.
     *
     * <p>O servidor valida se o jogador realmente esta na vez dele. Se nao
     * estiver, uma excecao remota e enviada ao cliente.</p>
     *
     * @param idJogador identificador unico do jogador
     * @return estado atualizado da mesa para esse jogador
     * @throws RemoteException se nao for a vez do jogador ou houver falha remota
     */
    EstadoPartida pedirCarta(String idJogador) throws RemoteException;

    /**
     * Encerra a vez do jogador atual.
     *
     * <p>Depois disso, o servidor passa a vez para o proximo jogador ou finaliza
     * a rodada caso todos ja tenham jogado.</p>
     *
     * @param idJogador identificador unico do jogador
     * @return estado atualizado da mesa para esse jogador
     * @throws RemoteException se nao for a vez do jogador ou houver falha remota
     */
    EstadoPartida pararJogada(String idJogador) throws RemoteException;

    /**
     * Busca o estado atual da mesa para um jogador especifico.
     *
     * <p>O cliente usa esse metodo periodicamente para atualizar cartas,
     * pontuacao, dealer revelado e status da rodada.</p>
     *
     * @param idJogador identificador unico do jogador
     * @return estado atual visto por esse jogador
     * @throws RemoteException se o jogador nao estiver na mesa ou houver falha remota
     */
    EstadoPartida obterEstado(String idJogador) throws RemoteException;

    /**
     * Lista todos os jogadores conectados a mesa.
     *
     * <p>Esse metodo alimenta a barra lateral da interface grafica, mostrando
     * nome, cartas, pontuacao, status e quem esta na vez.</p>
     *
     * @return lista com resumos dos jogadores conectados
     * @throws RemoteException se houver falha de comunicacao RMI
     */
    List<EstadoJogador> listarJogadores() throws RemoteException;

    /**
     * Remove um jogador da mesa.
     *
     * <p>Chamado quando o usuario clica em sair ou fecha a janela. Se o jogador
     * estava na rodada atual, o servidor ajusta a ordem dos turnos.</p>
     *
     * @param idJogador identificador unico do jogador
     * @throws RemoteException se houver falha de comunicacao RMI
     */
    void desconectar(String idJogador) throws RemoteException;
}
