package blackjack;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.UUID;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * Controlador RMI do cliente grafico.
 *
 * <p>Esta classe concentra a comunicacao remota com o servidor. A montagem e a
 * atualizacao visual da janela ficam em {@link ClienteGUI}.</p>
 */
public class Cliente {

    private final ClienteGUI gui = new ClienteGUI();
    private final Timer playersRefreshTimer = new Timer(750, event -> refreshTableAndState());

    private Jogo21Interface service;
    private ClienteCallback callback;
    private String playerName;
    private String playerDisplayName;
    private EstadoPartida currentState;

    /**
     * Abre a interface grafica na thread correta do Swing.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Cliente().show());
    }

    /**
     * Cria o controlador e liga os eventos da interface aos metodos RMI.
     */
    public Cliente() {
        bindActions();
        gui.updateActionButtons(false, false);
    }

    /** Mostra a janela principal. */
    private void show() {
        gui.setVisible(true);
    }

    /**
     * Liga cada acao da janela ao comportamento remoto correspondente.
     */
    private void bindActions() {
        gui.onConnect(event -> connect());
        gui.onLeaveTable(event -> leaveTable());
        gui.onHit(event -> runRemoteAction("Pedindo carta...", () -> service.pedirCarta(playerName)));
        gui.onStand(event -> runRemoteAction("Dealer jogando...", () -> service.pararJogada(playerName)));
        gui.onWindowClosing(this::disconnectFromServer);
    }

    /**
     * Conecta o jogador ao servidor RMI.
     */
    private void connect() {
        String requestedName = gui.promptPlayerName();
        if (requestedName == null) {
            return;
        }

        String displayName = requestedName.trim();
        if (displayName.isEmpty()) {
            displayName = "Jogador sem nome";
        }

        String host = gui.getHost().trim();
        int port;
        try {
            port = Integer.parseInt(gui.getPort().trim());
        } catch (NumberFormatException ex) {
            gui.showError("Porta invalida.", ex);
            return;
        }

        playerDisplayName = displayName;
        gui.setDisplayName(displayName);
        gui.prepareConnecting();

        new SwingWorker<EstadoPartida, Void>() {
            @Override
            protected EstadoPartida doInBackground() throws Exception {
                Registry registry = LocateRegistry.getRegistry(host, port);
                service = (Jogo21Interface) registry.lookup("ServicoJogo21");
                callback = new GuiCallback();
                playerName = playerDisplayName + "-" + UUID.randomUUID().toString().substring(0, 8);
                return service.conectar(playerName, playerDisplayName, callback);
            }

            @Override
            protected void done() {
                try {
                    currentState = get();
                    gui.clearLog();
                    gui.setIdentity("Jogador: " + playerDisplayName);
                    gui.appendLog("Conectado como " + playerDisplayName + ".");
                    gui.renderState(currentState);
                    refreshPlayersTable();
                    playersRefreshTimer.start();
                    gui.updateActionButtons(currentState.getStatus() == EstadoPartida.Status.EM_ANDAMENTO, isConnected());
                } catch (Exception ex) {
                    gui.showError("Nao foi possivel conectar ao servidor.", ex);
                    clearLocalConnection();
                    resetConnection();
                }
            }
        }.execute();
    }

    /**
     * Executa uma chamada remota que retorna um novo estado da partida.
     */
    private void runRemoteAction(String message, RemoteStateCall call) {
        gui.updateActionButtons(false, isConnected());
        gui.setStatusText(message);

        new SwingWorker<EstadoPartida, Void>() {
            @Override
            protected EstadoPartida doInBackground() throws Exception {
                return call.execute();
            }

            @Override
            protected void done() {
                try {
                    currentState = get();
                    gui.renderState(currentState);
                    refreshPlayersTable();
                    gui.updateActionButtons(currentState.getStatus() == EstadoPartida.Status.EM_ANDAMENTO, isConnected());
                } catch (Exception ex) {
                    gui.showError("Falha ao comunicar com o servidor.", ex);
                    gui.updateActionButtons(currentState != null && currentState.getStatus() == EstadoPartida.Status.EM_ANDAMENTO, isConnected());
                }
            }
        }.execute();
    }

    /**
     * Busca no servidor a lista atualizada de jogadores da mesa.
     */
    private void refreshPlayersTable() {
        if (service == null || playerName == null) {
            return;
        }

        new SwingWorker<List<EstadoJogador>, Void>() {
            @Override
            protected List<EstadoJogador> doInBackground() throws Exception {
                return service.listarJogadores(playerName);
            }

            @Override
            protected void done() {
                try {
                    gui.renderPlayersTable(get(), playerName);
                } catch (Exception ex) {
                    gui.appendLog("Nao foi possivel atualizar jogadores: " + ex.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Atualiza periodicamente a lista de jogadores e o estado do jogador local.
     */
    private void refreshTableAndState() {
        refreshPlayersTable();
        if (service == null || playerName == null) {
            return;
        }

        new SwingWorker<EstadoPartida, Void>() {
            @Override
            protected EstadoPartida doInBackground() throws Exception {
                return service.obterEstado(playerName);
            }

            @Override
            protected void done() {
                try {
                    currentState = get();
                    gui.renderState(currentState);
                    gui.updateActionButtons(currentState.getStatus() == EstadoPartida.Status.EM_ANDAMENTO, isConnected());
                } catch (Exception ex) {
                    gui.appendLog("Voce nao esta mais conectado a mesa.");
                    service = null;
                    playerName = null;
                    playersRefreshTimer.stop();
                    resetConnection();
                }
            }
        }.execute();
    }

    /**
     * Solicita ao servidor a remocao deste jogador da mesa.
     */
    private void disconnectFromServer() {
        playersRefreshTimer.stop();
        if (service != null && playerName != null) {
            try {
                service.desconectar(playerName);
            } catch (Exception ex) {
                System.err.println("[CLIENTE] Falha ao desconectar: " + ex.getMessage());
            }
        }
    }

    /**
     * Sai da mesa, limpa o estado remoto local e reseta a interface.
     */
    private void leaveTable() {
        disconnectFromServer();
        clearLocalConnection();
        gui.resetGameView();
        resetConnection();
        gui.appendLog("Voce saiu da mesa.");
    }

    /**
     * Retorna a tela ao estado inicial de conexao.
     */
    private void resetConnection() {
        gui.resetConnection(isConnected());
    }

    private boolean isConnected() {
        return service != null && playerName != null;
    }

    private void clearLocalConnection() {
        service = null;
        callback = null;
        playerName = null;
        playerDisplayName = null;
        currentState = null;
    }

    /**
     * Pequena interface funcional usada para padronizar chamadas remotas.
     */
    private interface RemoteStateCall {
        EstadoPartida execute() throws Exception;
    }

    /**
     * Callback remoto recebido pelo servidor para enviar notificacoes ao cliente.
     */
    private class GuiCallback extends UnicastRemoteObject implements ClienteCallback {
        private static final long serialVersionUID = 1L;

        GuiCallback() throws RemoteException {
            super();
        }

        @Override
        public void notificar(String mensagem) throws RemoteException {
            SwingUtilities.invokeLater(() -> {
                if (mensagem.startsWith("RESULTADO_RODADA|")) {
                    String resumo = mensagem.substring("RESULTADO_RODADA|".length());
                    gui.appendLog("[Rodada]\n" + resumo);
                    gui.showRoundResultAnimation(resumo);
                } else {
                    gui.appendLog("[Servidor] " + mensagem);
                }
            });
        }
    }
}
