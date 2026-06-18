package blackjack;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
/**
 * Cliente RMI para o jogo de Blackjack (Jogo 21).
 * Permite que o usuário se conecte ao servidor, jogue uma partida e receba atualizações em tempo real.
 */
public class Cliente {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // ─── Conexão com o Registro RMI ───────────────────────────────────
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            Jogo21Interface servicoRemoto = (Jogo21Interface) registry.lookup("ServicoJogo21");

           
            System.out.print("Digite seu nome: ");
            String nome = scanner.nextLine().trim();
            if (nome.isEmpty()) nome = "Jogador";

            // ─── Criação e registro do callback ───────────────────────────────
            ClienteCallbackImpl callback = new ClienteCallbackImpl();

            // ─── Início da partida ────────────────────────────────────────────
            EstadoPartida estado = servicoRemoto.conectar(nome, callback);
            exibirEstado(estado);

            // ─── Loop principal de interação ──────────────────────────────────
            while (!estado.isPartidaEncerrada()) {
                System.out.println("\nO que deseja fazer?");
                System.out.println("  [1] Pedir carta (Hit)");
                System.out.println("  [2] Manter       (Stand)");
                System.out.print("Sua escolha: ");

                String entrada = scanner.nextLine().trim();

                switch (entrada) {
                    case "1" -> {
                        // Hit: solicita nova carta
                        estado = servicoRemoto.pedirCarta(nome);
                        exibirEstado(estado);
                    }
                    case "2" -> {
                        // Stand: encerra o turno do jogador; dealer joga automaticamente
                        System.out.println("\n>> Você parou. O dealer vai jogar agora...");
                        estado = servicoRemoto.pararJogada(nome);
                        exibirEstado(estado);
                    }
                    default -> System.out.println("Opção inválida. Digite 1 para Hit ou 2 para Stand.");
                }
            }

           System.out.println("\nObrigado por jogar, " + nome + "! Até a próxima.");

        } catch (Exception e) {
            System.err.println("[ERRO] Falha na comunicação com o servidor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    
    private static void exibirEstado(EstadoPartida estado) {
        System.out.println(estado.toString());
    }
}
