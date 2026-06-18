package blackjack;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class Servidor {

    public static void main(String[] args) {
        try {

            Jogo21InterfaceImpl objetoServicoReal = new Jogo21InterfaceImpl();


            Registry registry = LocateRegistry.createRegistry(1099);

            registry.bind("ServicoJogo21", objetoServicoReal);

            System.out.println("[SERVIDOR] Servidor de Blackjack iniciado e registrado com sucesso.");
            System.out.println("[SERVIDOR] Aguardando conexões de clientes...");
        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao iniciar o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
