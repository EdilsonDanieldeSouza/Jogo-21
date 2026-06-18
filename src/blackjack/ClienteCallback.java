package blackjack;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClienteCallback extends Remote {

   
    void notificar(String mensagem) throws RemoteException;
}
