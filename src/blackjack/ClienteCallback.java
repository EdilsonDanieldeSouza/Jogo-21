package blackjack;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Contrato remoto usado pelo servidor para enviar mensagens ao cliente.
 *
 * <p>Normalmente o cliente chama metodos do servidor. Este callback permite o
 * caminho inverso: o servidor chama {@link #notificar(String)} em cada cliente
 * para avisar eventos da mesa.</p>
 */
public interface ClienteCallback extends Remote {

    /**
     * Recebe uma mensagem enviada pelo servidor.
     *
     * @param mensagem texto que deve ser exibido pelo cliente
     * @throws RemoteException se o cliente nao puder receber a mensagem
     */
    void notificar(String mensagem) throws RemoteException;
}
