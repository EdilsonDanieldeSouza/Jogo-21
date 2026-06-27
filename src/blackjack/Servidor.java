package blackjack;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Classe responsavel por iniciar o servidor RMI do jogo.
 *
 * <p>O servidor cria uma instancia real de {@link Jogo21InterfaceImpl},
 * abre um registry RMI na porta 1099 e publica o servico com o nome
 * {@code ServicoJogo21}. Os clientes usam esse nome para localizar a mesa.</p>
 */
public class Servidor {

    /**
     * Ponto de entrada do servidor.
     *
     * @param args argumentos de linha de comando, nao utilizados neste projeto
     */
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
