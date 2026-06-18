package blackjack;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementação do callback no lado do cliente.
 * Estende UnicastRemoteObject para ser exportado como objeto RMI remoto,
 * permitindo que o servidor invoque métodos nesta instância diretamente.
 */
public class ClienteCallbackImpl extends UnicastRemoteObject implements ClienteCallback {

    private static final long serialVersionUID = 1L;

    public ClienteCallbackImpl() throws RemoteException {
        super();
    }
    
    @Override
    public void notificar(String mensagem) throws RemoteException {
        System.out.println("[NOTIFICAÇÃO] " + mensagem);
    }
}
