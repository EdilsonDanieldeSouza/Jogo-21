package blackjack;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementacao simples do callback no lado do cliente.
 *
 * <p>Ela imprime notificacoes no console. O cliente grafico principal usa uma
 * implementacao interna chamada {@code GuiCallback}, mas esta classe continua
 * util para testes ou clientes sem interface grafica.</p>
 */
public class ClienteCallbackImpl extends UnicastRemoteObject implements ClienteCallback {

    private static final long serialVersionUID = 1L;

    /**
     * Exporta este objeto como objeto remoto RMI.
     *
     * @throws RemoteException se o objeto nao puder ser exportado
     */
    public ClienteCallbackImpl() throws RemoteException {
        super();
    }

    /**
     * Exibe no console a mensagem recebida do servidor.
     */
    @Override
    public void notificar(String mensagem) throws RemoteException {
        System.out.println("[NOTIFICACAO] " + mensagem);
    }
}
