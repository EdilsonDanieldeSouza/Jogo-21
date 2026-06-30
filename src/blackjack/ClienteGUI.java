package blackjack;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 * Janela Swing do jogo de Blackjack.
 *
 * <p>Esta classe concentra apenas componentes visuais, renderizacao de estado e
 * dialogos. A comunicacao RMI fica no controlador {@link Cliente}.</p>
 */
public class ClienteGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final Color TABLE_GREEN = new Color(24, 96, 62);
    private static final Color TABLE_DARK = new Color(13, 54, 36);
    private static final Color CARD_RED = new Color(176, 35, 35);
    private static final Color CARD_BLACK = new Color(30, 35, 42);

    private final JTextField hostField = new JTextField("localhost", 12);
    private final JTextField portField = new JTextField("1099", 5);
    private final JTextField nameField = new JTextField("Jogador", 12);
    private final JButton connectButton = new JButton("Conectar");
    private final JButton leaveButton = new JButton("Sair da mesa");
    private final JButton hitButton = new JButton("Pedir carta");
    private final JButton standButton = new JButton("Parar");

    private final JLabel titleLabel = new JLabel("Jogo 21");
    private final JLabel identityLabel = new JLabel("Voce ainda nao entrou na mesa");
    private final JLabel statusLabel = new JLabel("Conecte-se ao servidor para iniciar.");
    private final JLabel dealerScoreLabel = new JLabel("Dealer: -");
    private final JLabel playerScoreLabel = new JLabel("Jogador: -");
    private final JLabel betInfoLabel = new JLabel("Fichas: - | Aposta: - | Historico: -");
    private final JPanel dealerCardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
    private final JPanel playerCardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
    private final JPanel tablePlayersPanel = new JPanel();
    private final JTextArea logArea = new JTextArea(6, 40);

    private String lastStatusMessage = "";

    public ClienteGUI() {
        super("Jogo de 21 - Trabalho 3");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(880, 620));
        setLocationByPlatform(true);
        buildLayout();
        pack();
    }

    public void onConnect(ActionListener listener) {
        connectButton.addActionListener(listener);
    }

    public void onLeaveTable(ActionListener listener) {
        leaveButton.addActionListener(listener);
    }

    public void onHit(ActionListener listener) {
        hitButton.addActionListener(listener);
    }

    public void onStand(ActionListener listener) {
        standButton.addActionListener(listener);
    }

    public void onWindowClosing(Runnable action) {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                action.run();
            }
        });
    }

    public String promptPlayerName() {
        return JOptionPane.showInputDialog(
                this,
                "Digite o nome do jogador:",
                nameField.getText().trim().isEmpty() ? "Jogador sem nome" : nameField.getText().trim()
        );
    }

    public String getHost() {
        return hostField.getText();
    }

    public String getPort() {
        return portField.getText();
    }

    public void setDisplayName(String displayName) {
        nameField.setText(displayName);
    }

    public void setIdentity(String text) {
        identityLabel.setText(text);
    }

    public void setStatusText(String text) {
        statusLabel.setText(text);
    }

    public void prepareConnecting() {
        updateActionButtons(false, false);
        connectButton.setEnabled(false);
        setConnectionFieldsEnabled(false);
        setStatusText("Conectando ao servidor...");
    }

    public void resetConnection(boolean connected) {
        setConnectionFieldsEnabled(!connected);
        updateActionButtons(false, connected);
        if (!connected) {
            setStatusText("Conecte-se ao servidor para iniciar.");
        }
    }

    public void resetGameView() {
        lastStatusMessage = "";
        dealerCardsPanel.removeAll();
        playerCardsPanel.removeAll();
        renderPlayersTable(List.of(), null);
        playerScoreLabel.setText("Jogador: -");
        dealerScoreLabel.setText("Dealer: -");
        betInfoLabel.setText("Fichas: - | Aposta: - | Historico: -");
        identityLabel.setText("Voce ainda nao entrou na mesa");
        revalidate();
        repaint();
    }

    public void updateActionButtons(boolean playing, boolean connected) {
        hitButton.setEnabled(playing);
        standButton.setEnabled(playing);
        leaveButton.setEnabled(connected);
        connectButton.setEnabled(!connected);
    }

    public void clearLog() {
        logArea.setText("");
    }

    public void appendLog(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void showError(String title, Exception ex) {
        String detail = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = ex.getClass().getSimpleName();
        }
        appendLog(title + " " + detail);
        JOptionPane.showMessageDialog(this, title + "\n" + detail, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Atualiza a mesa com o estado recebido do servidor.
     */
    public void renderState(EstadoPartida state) {
        renderDealerCards(state);
        renderCards(playerCardsPanel, state.getCartasJogador(), false);

        playerScoreLabel.setText("Jogador: " + state.getPontuacaoJogador());
        dealerScoreLabel.setText(state.isDealerRevelado() ? "Dealer: " + state.getPontuacaoDealer() : "Dealer: carta oculta");
        betInfoLabel.setText("Fichas: " + state.getSaldoJogador() + " | Aposta: " + state.getApostaAtual() + " | Historico: " + state.getVitoriasJogador() + "V/" + state.getEmpatesJogador() + "E/" + state.getDerrotasJogador() + "D");

        String message = messageForStatus(state.getStatus());
        statusLabel.setText(message);
        if (!message.equals(lastStatusMessage)) {
            appendLog(message);
            lastStatusMessage = message;
        }

        revalidate();
        repaint();
    }

    /**
     * Redesenha a barra lateral com os jogadores recebidos do servidor.
     */
    public void renderPlayersTable(List<EstadoJogador> jogadores, String currentPlayerId) {
        tablePlayersPanel.removeAll();

        if (jogadores.isEmpty()) {
            tablePlayersPanel.add(emptyPlayersLabel());
        } else {
            for (EstadoJogador jogador : jogadores) {
                tablePlayersPanel.add(playerSummaryPanel(jogador, currentPlayerId));
            }
        }

        tablePlayersPanel.revalidate();
        tablePlayersPanel.repaint();
    }

    /**
     * Mostra o resumo final da rodada por 5 segundos.
     */
    public void showRoundResultAnimation(String resumo) {
        JDialog dialog = new JDialog(this, "Fim da rodada", false);
        dialog.setSize(460, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(18, 22, 18, 22));
        panel.setBackground(new Color(18, 72, 48));

        JLabel title = new JLabel("Resultado da rodada", JLabel.CENTER);
        title.setForeground(new Color(245, 215, 95));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        panel.add(title, BorderLayout.NORTH);

        JTextArea summary = new JTextArea(resumo);
        summary.setEditable(false);
        summary.setOpaque(false);
        summary.setForeground(Color.WHITE);
        summary.setFont(summary.getFont().deriveFont(Font.BOLD, 15f));
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        panel.add(summary, BorderLayout.CENTER);

        JLabel footer = new JLabel("Proxima rodada sendo preparada...", JLabel.CENTER);
        footer.setForeground(new Color(220, 235, 226));
        panel.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);

        Timer closeTimer = new Timer(5000, event -> dialog.dispose());
        closeTimer.setRepeats(false);
        closeTimer.start();
    }

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(TABLE_GREEN);
        setContentPane(root);

        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildTable(), BorderLayout.CENTER);
        root.add(buildPlayersSidebar(), BorderLayout.EAST);
        root.add(buildBottomBar(), BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setBorder(new EmptyBorder(14, 18, 14, 18));
        top.setBackground(TABLE_DARK);

        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));

        JPanel titlePanel = new JPanel(new BorderLayout(0, 4));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        identityLabel.setForeground(new Color(220, 235, 226));
        identityLabel.setFont(identityLabel.getFont().deriveFont(Font.BOLD, 13f));
        titlePanel.add(identityLabel, BorderLayout.SOUTH);
        top.add(titlePanel, BorderLayout.WEST);

        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        connectionPanel.setOpaque(false);
        connectionPanel.add(fieldGroup("Host", hostField));
        connectionPanel.add(fieldGroup("Porta", portField));
        connectionPanel.add(connectButton);
        connectionPanel.add(leaveButton);
        top.add(connectionPanel, BorderLayout.EAST);

        return top;
    }

    private JPanel fieldGroup(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setForeground(new Color(220, 235, 226));
        panel.add(fieldLabel, BorderLayout.NORTH);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 30));
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTable() {
        JPanel table = new JPanel(new GridBagLayout());
        table.setBackground(TABLE_GREEN);
        table.setBorder(new EmptyBorder(16, 24, 16, 24));

        dealerCardsPanel.setOpaque(false);
        playerCardsPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 12, 0);

        gbc.gridy = 0;
        table.add(sectionHeader("Mesa do dealer", dealerScoreLabel), gbc);

        gbc.gridy = 1;
        gbc.weighty = 0.45;
        gbc.fill = GridBagConstraints.BOTH;
        table.add(dealerCardsPanel, gbc);

        gbc.gridy = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        table.add(statusPanel(), gbc);

        gbc.gridy = 3;
        table.add(sectionHeader("Sua mao", playerScoreLabel), gbc);

        gbc.gridy = 4;
        gbc.weighty = 0.45;
        gbc.fill = GridBagConstraints.BOTH;
        table.add(playerCardsPanel, gbc);

        return table;
    }

    private JPanel buildPlayersSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 10));
        sidebar.setPreferredSize(new Dimension(330, 100));
        sidebar.setBackground(new Color(18, 72, 48));
        sidebar.setBorder(new EmptyBorder(16, 14, 16, 14));

        JLabel title = new JLabel("Jogadores na mesa");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        sidebar.add(title, BorderLayout.NORTH);

        tablePlayersPanel.setLayout(new BoxLayout(tablePlayersPanel, BoxLayout.Y_AXIS));
        tablePlayersPanel.setOpaque(false);
        tablePlayersPanel.add(emptyPlayersLabel());

        JScrollPane scrollPane = new JScrollPane(tablePlayersPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        sidebar.add(scrollPane, BorderLayout.CENTER);

        return sidebar;
    }

    private JPanel sectionHeader(String title, JLabel scoreLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 17f));

        scoreLabel.setForeground(new Color(236, 244, 239));
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 17f));

        panel.add(label, BorderLayout.WEST);
        panel.add(scoreLabel, BorderLayout.EAST);
        return panel;
    }

    private JPanel statusPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 0, 12, 0));

        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 20f));
        panel.add(statusLabel, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(100, 115));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildBottomBar() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 14));
        bottom.setBackground(TABLE_DARK);

        hitButton.setPreferredSize(new Dimension(150, 38));
        standButton.setPreferredSize(new Dimension(150, 38));

        betInfoLabel.setForeground(new Color(236, 244, 239));
        betInfoLabel.setFont(betInfoLabel.getFont().deriveFont(Font.BOLD, 14f));
        bottom.add(hitButton);
        bottom.add(standButton);
        bottom.add(betInfoLabel);

        return bottom;
    }

    private void renderDealerCards(EstadoPartida state) {
        renderCards(dealerCardsPanel, state.getCartasDealer(), false);
        if (!state.isDealerRevelado() && !state.getCartasDealer().isEmpty()) {
            dealerCardsPanel.add(new CardView(null, true));
            dealerCardsPanel.revalidate();
            dealerCardsPanel.repaint();
        }
    }

    private void renderCards(JPanel panel, List<Carta> cards, boolean hideAfterFirst) {
        panel.removeAll();
        for (int i = 0; i < cards.size(); i++) {
            boolean hidden = hideAfterFirst && i > 0;
            panel.add(new CardView(cards.get(i), hidden));
        }
        panel.revalidate();
        panel.repaint();
    }

    private JLabel emptyPlayersLabel() {
        JLabel label = new JLabel("Nenhum jogador conectado.");
        label.setForeground(new Color(220, 235, 226));
        label.setBorder(new EmptyBorder(10, 0, 0, 0));
        return label;
    }

    private JPanel playerSummaryPanel(EstadoJogador jogador, String currentPlayerId) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(70, 130, 95)),
                new EmptyBorder(10, 0, 12, 0)));

        boolean isMe = currentPlayerId != null && jogador.getId().equals(currentPlayerId);
        String pontuacao = jogador.getPontuacao() >= 0 ? jogador.getPontuacao() + " pts" : "cartas ocultas";
        JLabel header = new JLabel(jogador.getNome() + " - " + pontuacao
                + " - " + jogador.getVitorias() + "V/"
                + jogador.getEmpates() + "E/" + jogador.getDerrotas() + "D");
        if (isMe) {
            header.setText("VOCE - " + jogador.getNome() + " - " + pontuacao
                    + " - " + jogador.getSaldo() + " fichas");
        }
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(header, BorderLayout.NORTH);

        if (isMe) {
            panel.setOpaque(true);
            panel.setBackground(new Color(38, 118, 77));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(245, 215, 95), 2),
                    new EmptyBorder(10, 8, 12, 8)));
        }

        JPanel cards = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cards.setOpaque(false);
        if (jogador.getCartas().isEmpty() && !isMe && jogador.isNaRodadaAtual()) {
            cards.add(new HiddenMiniCardView());
            cards.add(new HiddenMiniCardView());
        } else {
            for (Carta carta : jogador.getCartas()) {
                cards.add(new MiniCardView(carta));
            }
        }
        panel.add(cards, BorderLayout.CENTER);

        JLabel status = new JLabel(statusText(jogador)
                + " | Aposta: " + jogador.getApostaAtual()
                + " | " + jogador.getVitorias() + "V/"
                + jogador.getEmpates() + "E/" + jogador.getDerrotas() + "D");
        status.setForeground(new Color(220, 235, 226));
        panel.add(status, BorderLayout.SOUTH);

        return panel;
    }

    private void setConnectionFieldsEnabled(boolean enabled) {
        hostField.setEnabled(enabled);
        portField.setEnabled(enabled);
        nameField.setEnabled(enabled);
    }

    private String messageForStatus(EstadoPartida.Status status) {
        return switch (status) {
            case EM_ANDAMENTO -> "Sua vez: escolha pedir carta ou parar.";
            case JOGADOR_ESTOUROU -> "Voce estourou. Dealer venceu.";
            case DEALER_ESTOUROU -> "Dealer estourou. Voce venceu!";
            case JOGADOR_VENCEU -> "Voce venceu!";
            case DEALER_VENCEU -> "Dealer venceu.";
            case AGUARDANDO_PROXIMA_RODADA -> "Mesa em andamento. Voce entra na proxima rodada.";
            case AGUARDANDO_VEZ -> "Aguarde sua vez.";
            case JOGADOR_PAROU -> "Voce parou. Aguardando o fim da rodada.";
            case EMPATE -> "Empate.";
        };
    }

    private String statusText(EstadoJogador jogador) {
        if (!jogador.isNaRodadaAtual()) {
            return "Aguardando proxima rodada";
        }
        if (jogador.isVezAtual()) {
            return "Vez atual";
        }
        return switch (jogador.getStatus()) {
            case EM_ANDAMENTO -> "Jogando";
            case AGUARDANDO_VEZ -> "Aguardando vez";
            case AGUARDANDO_PROXIMA_RODADA -> "Aguardando proxima rodada";
            case JOGADOR_PAROU -> "Parou";
            case JOGADOR_ESTOUROU -> "Estourou";
            case DEALER_ESTOUROU -> "Venceu";
            case JOGADOR_VENCEU -> "Venceu";
            case DEALER_VENCEU -> "Dealer venceu";
            case EMPATE -> "Empate.";
        };
    }

    private static class MiniCardView extends JPanel {
        private static final long serialVersionUID = 1L;

        MiniCardView(Carta card) {
            setPreferredSize(new Dimension(46, 64));
            setMinimumSize(new Dimension(46, 64));
            setMaximumSize(new Dimension(46, 64));
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(6, 4, 6, 6));

            Color textColor = isRed(card) ? CARD_RED : CARD_BLACK;

            JLabel value = new JLabel(card.getValor().getNome(), JLabel.CENTER);
            value.setForeground(textColor);
            value.setFont(value.getFont().deriveFont(Font.BOLD, 13f));

            JLabel suit = new JLabel(suitSymbol(card), JLabel.CENTER);
            suit.setForeground(textColor);
            suit.setFont(suit.getFont().deriveFont(Font.BOLD, 20f));

            add(value, BorderLayout.NORTH);
            add(suit, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth() - 8;
            int height = getHeight() - 8;
            g.setColor(new Color(0, 0, 0, 55));
            g.fillRoundRect(6, 7, width, height, 10, 10);
            g.setColor(new Color(250, 250, 246));
            g.fillRoundRect(2, 2, width, height, 10, 10);
            g.setColor(new Color(218, 218, 210));
            g.drawRoundRect(2, 2, width, height, 10, 10);

            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class HiddenMiniCardView extends JPanel {
        private static final long serialVersionUID = 1L;

        HiddenMiniCardView() {
            setPreferredSize(new Dimension(46, 64));
            setMinimumSize(new Dimension(46, 64));
            setMaximumSize(new Dimension(46, 64));
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(6, 4, 6, 6));

            JLabel back = new JLabel("\u2660", JLabel.CENTER);
            back.setForeground(Color.WHITE);
            back.setFont(back.getFont().deriveFont(Font.BOLD, 22f));
            add(back, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth() - 8;
            int height = getHeight() - 8;
            g.setColor(new Color(0, 0, 0, 55));
            g.fillRoundRect(6, 7, width, height, 10, 10);
            GradientPaint back = new GradientPaint(2, 2, new Color(34, 71, 151),
                    2 + width, 2 + height, new Color(16, 35, 92));
            g.setPaint(back);
            g.fillRoundRect(2, 2, width, height, 10, 10);
            g.setColor(new Color(255, 255, 255, 70));
            g.drawRoundRect(8, 8, width - 12, height - 12, 8, 8);

            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class CardView extends JPanel {
        private static final long serialVersionUID = 1L;

        CardView(Carta card, boolean hidden) {
            setPreferredSize(new Dimension(102, 140));
            setMinimumSize(new Dimension(102, 140));
            setMaximumSize(new Dimension(102, 140));
            setLayout(new GridBagLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 10, 10, 14));

            if (hidden) {
                addCenteredLabel("\u2660", Color.WHITE, 42f);
                return;
            }

            Color textColor = isRed(card) ? CARD_RED : CARD_BLACK;

            JPanel content = new JPanel(new GridBagLayout());
            content.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            content.add(cardLabel(card.getValor().getNome(), textColor, 24f, JLabel.LEFT), gbc);

            gbc.gridy = 1;
            gbc.weighty = 1;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.BOTH;
            content.add(cardLabel(suitSymbol(card), textColor, 46f, JLabel.CENTER), gbc);

            gbc.gridy = 2;
            gbc.weighty = 0;
            gbc.anchor = GridBagConstraints.SOUTHEAST;
            content.add(cardLabel(suitSymbol(card) + " " + card.getValor().getNome(), textColor, 20f, JLabel.RIGHT), gbc);

            add(content, fillConstraints());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = 8;
            int y = 8;
            int width = getWidth() - 18;
            int height = getHeight() - 18;

            g.setColor(new Color(0, 0, 0, 70));
            g.fillRoundRect(x + 6, y + 7, width, height, 16, 16);

            if (getComponentCount() > 0 && getComponent(0) instanceof JLabel) {
                GradientPaint back = new GradientPaint(x, y, new Color(34, 71, 151),
                        x + width, y + height, new Color(16, 35, 92));
                g.setPaint(back);
                g.fillRoundRect(x, y, width, height, 16, 16);
                g.setColor(new Color(255, 255, 255, 70));
                g.drawRoundRect(x + 8, y + 8, width - 16, height - 16, 12, 12);
            } else {
                g.setColor(new Color(250, 250, 246));
                g.fillRoundRect(x, y, width, height, 16, 16);
                g.setColor(new Color(218, 218, 210));
                g.drawRoundRect(x, y, width, height, 16, 16);
            }

            g.dispose();
            super.paintComponent(graphics);
        }

        private void addCenteredLabel(String text, Color color, float size) {
            add(cardLabel(text, color, size, JLabel.CENTER), fillConstraints());
        }

        private static GridBagConstraints fillConstraints() {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            return gbc;
        }

        private static JLabel cardLabel(String text, Color color, float size, int alignment) {
            JLabel label = new JLabel(text, alignment);
            label.setForeground(color);
            label.setFont(label.getFont().deriveFont(Font.BOLD, size));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            return label;
        }
    }

    private static boolean isRed(Carta card) {
        return card.getNipe() == Carta.Nipe.COPAS || card.getNipe() == Carta.Nipe.OUROS;
    }

    private static String suitSymbol(Carta card) {
        return switch (card.getNipe()) {
            case COPAS -> "\u2665";
            case OUROS -> "\u2666";
            case PAUS -> "\u2663";
            case ESPADAS -> "\u2660";
        };
    }
}
