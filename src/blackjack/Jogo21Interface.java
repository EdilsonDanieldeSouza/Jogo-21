package blackjack;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface remota do serviço de Blackjack.
 * Define o contrato de comunicação entre cliente e servidor via RMI.
 * Todos os métodos devem lançar RemoteException.
 */
public interface Jogo21Interface extends Remote {

    EstadoPartida conectar(String nome, ClienteCallback callback) throws RemoteException;

    
    EstadoPartida pedirCarta(String nome) throws RemoteException;

   
    EstadoPartida pararJogada(String nome) throws RemoteException;
}
