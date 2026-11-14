
package pitiupi.quiz.ui;

import pitiupi.quiz.model.QuizQuestion;
import pitiupi.quiz.model.QuizRoom;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.IntConsumer;

public class QuizPanel extends JPanel {

    private static final Color BG = new Color(13, 20, 34);
    private static final Color SURFACE = new Color(23, 34, 54);
    private static final Color SURFACE_ALT = new Color(29, 43, 66);
    private static final Color BORDER = new Color(48, 65, 91);

    private static final Color TEXT = new Color(239, 245, 255);
    private static final Color MUTED = new Color(157, 174, 198);

    private static final Color ACCENT = new Color(54, 211, 174);
    private static final Color BLUE = new Color(83, 145, 255);


    private final DefaultListModel<QuizRoom> roomsModel =
            new DefaultListModel<>();

    private final JList<QuizRoom> roomsList =
            new JList<>(roomsModel);

    private final DefaultListModel<String> participantsModel =
            new DefaultListModel<>();

    private final JList<String> participantsList =
            new JList<>(participantsModel);


    private final JButton hostButton =
            new ActionButton("Criar sala", ACCENT, new Color(8, 35, 39));

    private final JButton manageQuestionsButton =
            new ActionButton("Perguntas", SURFACE_ALT, TEXT);

    private final JButton refreshButton =
            new ActionButton("Procurar salas", SURFACE_ALT, TEXT);

    private final JButton joinButton =
            new ActionButton("Entrar na sala", BLUE, Color.WHITE);

    private final JButton startGameButton =
            new ActionButton("Iniciar partida", ACCENT, new Color(8, 35, 39));

    private final JButton[] answerButtons = {
            new AnswerButton("A", new Color(55, 111, 204)),
            new AnswerButton("B", new Color(127, 91, 205)),
            new AnswerButton("C", new Color(27, 151, 132)),
            new AnswerButton("D", new Color(194, 126, 48))
    };

    private final JLabel gameQuestion =
            new JLabel("A partida ainda não começou.");

    private final JLabel gameTimer =
            new JLabel("20s");

    private final JLabel gameFeedback =
            new JLabel("Quando a partida começar, as perguntas aparecerão aqui.");

    private final JCheckBox hostParticipates =
            new JCheckBox("Host também joga", true);

    private final JLabel lobbyTitle =
            new JLabel("Nenhuma sala hospedada");

    private final JLabel lobbyCount =
            new JLabel("0/8");

    private final JLabel status =
            new JLabel("Pronto para procurar partidas na rede.");

    private JPanel gameCard;
    private JPanel questionView;
    private JPanel answerOptionsPanel;
    private JPanel rankingView;

    private JLabel rankingContent;

    private CardLayout gameCards;


    public QuizPanel() {
        super(new BorderLayout(0, 14));

        setBackground(BG);
        setBorder(new EmptyBorder(18, 20, 16, 20));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel discovery = buildRoomsCard();
        JPanel lobby = buildLobbyCard();

        JSplitPane columns = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                discovery,
                lobby
        );

        columns.setResizeWeight(0.52);
        columns.setDividerSize(10);
        columns.setBorder(null);
        columns.setOpaque(false);
        columns.setBackground(BG);

        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setOpaque(false);

        main.add(columns, BorderLayout.CENTER);
        main.add(buildGameCard(), BorderLayout.SOUTH);

        add(main, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        setLobbyState(false, "", true, List.of(), 8);

        styleList(roomsList);
        styleList(participantsList);
    }


    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 4));
        header.setOpaque(false);

        JPanel branding = new JPanel();
        branding.setOpaque(false);
        branding.setLayout(new BoxLayout(branding, BoxLayout.Y_AXIS));

        JLabel eyebrow = label(
                "PITIUPI  /  JOGOS EM REDE",
                10,
                ACCENT,
                Font.BOLD
        );

        JLabel title = label(
                "Quiz P2P",
                27,
                TEXT,
                Font.BOLD
        );

        JLabel subtitle = label(
                "Crie uma sala, reúna os jogadores e teste seus conhecimentos.",
                12,
                MUTED,
                Font.PLAIN
        );

        branding.add(eyebrow);
        branding.add(Box.createVerticalStrut(3));
        branding.add(title);
        branding.add(Box.createVerticalStrut(3));
        branding.add(subtitle);

        header.add(branding, BorderLayout.WEST);

        JPanel actions = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 8)
        );

        actions.setOpaque(false);

        hostParticipates.setOpaque(false);
        hostParticipates.setForeground(TEXT);
        hostParticipates.setFont(new Font("Dialog", Font.PLAIN, 11));
        hostParticipates.setFocusPainted(false);

        actions.add(hostParticipates);
        actions.add(manageQuestionsButton);
        actions.add(hostButton);
        actions.add(refreshButton);

        header.add(actions, BorderLayout.EAST);

        return header;
    }

    private JPanel buildRoomsCard() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(8, 12));

        card.add(
                sectionHeader(
                        "SALAS DISPONÍVEIS",
                        "Partidas anunciadas na rede local"
                ),
                BorderLayout.NORTH
        );

        roomsList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        roomsList.setCellRenderer(new RoomRenderer());

        card.add(scroll(roomsList), BorderLayout.CENTER);

        JLabel hint = label(
                "Selecione uma sala para participar.",
                11,
                MUTED,
                Font.PLAIN
        );

        card.add(hint, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildLobbyCard() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(8, 12));

        card.add(
                sectionHeader(
                        "SEU LOBBY",
                        "Gerencie os participantes da sua sala"
                ),
                BorderLayout.NORTH
        );

        JPanel content = new JPanel(new BorderLayout(6, 10));
        content.setOpaque(false);

        JPanel info = new JPanel(new BorderLayout(8, 4));
        info.setOpaque(false);

        lobbyTitle.setForeground(TEXT);
        lobbyTitle.setFont(new Font("Dialog", Font.BOLD, 13));

        lobbyCount.setForeground(ACCENT);
        lobbyCount.setFont(new Font("Dialog", Font.BOLD, 15));

        info.add(lobbyTitle, BorderLayout.CENTER);
        info.add(lobbyCount, BorderLayout.EAST);

        content.add(info, BorderLayout.NORTH);

        participantsList.setVisibleRowCount(5);
        participantsList.setCellRenderer(new ParticipantRenderer());

        content.add(scroll(participantsList), BorderLayout.CENTER);

        card.add(content, BorderLayout.CENTER);

        JPanel controls = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 6, 0)
        );

        controls.setOpaque(false);
        controls.add(startGameButton);

        card.add(controls, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildGameCard() {
        gameCard = cardPanel();

        gameCards = new CardLayout();
        gameCard.setLayout(gameCards);

        questionView = buildQuestionView();
        JPanel waitingView = buildWaitingView();
        rankingView = buildRankingView();

        gameCard.add(waitingView, "waiting");
        gameCard.add(questionView, "question");
        gameCard.add(rankingView, "ranking");

        gameCards.show(gameCard, "waiting");
        gameCard.setVisible(false);

        return gameCard;
    }

    private JPanel buildQuestionView() {
        JPanel view = new JPanel(new BorderLayout(12, 12));
        view.setOpaque(false);

        JPanel heading = new JPanel(new BorderLayout(10, 4));
        heading.setOpaque(false);

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));

        labels.add(label("ÁREA DA PARTIDA", 10, ACCENT, Font.BOLD));

        gameQuestion.setForeground(TEXT);
        gameQuestion.setFont(new Font("Dialog", Font.BOLD, 17));

        labels.add(Box.createVerticalStrut(5));
        labels.add(gameQuestion);

        heading.add(labels, BorderLayout.CENTER);

        gameTimer.setOpaque(true);
        gameTimer.setBackground(new Color(39, 59, 83));
        gameTimer.setForeground(ACCENT);
        gameTimer.setFont(new Font("Dialog", Font.BOLD, 17));
        gameTimer.setHorizontalAlignment(SwingConstants.CENTER);
        gameTimer.setBorder(new EmptyBorder(8, 14, 8, 14));

        heading.add(gameTimer, BorderLayout.EAST);

        view.add(heading, BorderLayout.NORTH);

        answerOptionsPanel = new JPanel(
                new GridLayout(2, 2, 9, 9)
        );

        answerOptionsPanel.setOpaque(false);

        for (JButton button : answerButtons) {
            answerOptionsPanel.add(button);
        }

        view.add(answerOptionsPanel, BorderLayout.CENTER);

        gameFeedback.setForeground(MUTED);
        gameFeedback.setFont(new Font("Dialog", Font.PLAIN, 12));

        view.add(gameFeedback, BorderLayout.SOUTH);

        return view;
    }

    private JPanel buildWaitingView() {
        JPanel waitingView = new JPanel(new GridBagLayout());
        waitingView.setOpaque(false);

        JLabel waiting = label(
                "Aguardando o início da partida...",
                15,
                MUTED,
                Font.BOLD
        );

        waitingView.add(waiting);

        return waitingView;
    }

    private JPanel buildRankingView() {
        JPanel view = new JPanel(new BorderLayout(8, 12));
        view.setOpaque(false);

        JLabel title = label(
                "RANKING FINAL",
                12,
                ACCENT,
                Font.BOLD
        );

        view.add(title, BorderLayout.NORTH);

        rankingContent = new JLabel(
                "<html>A classificação aparecerá aqui.</html>"
        );

        rankingContent.setForeground(TEXT);
        rankingContent.setFont(new Font("Dialog", Font.PLAIN, 15));

        view.add(rankingContent, BorderLayout.CENTER);

        return view;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);

        JPanel statusDot = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 7, 0)
        );

        statusDot.setOpaque(false);

        JLabel dot = label("●", 10, ACCENT, Font.BOLD);

        status.setForeground(MUTED);
        status.setFont(new Font("Dialog", Font.PLAIN, 11));

        statusDot.add(dot);
        statusDot.add(status);

        footer.add(statusDot, BorderLayout.CENTER);
        footer.add(joinButton, BorderLayout.EAST);

        return footer;
    }

    private JPanel cardPanel() {
        JPanel panel = new RoundedPanel(SURFACE, 16);
        panel.setBorder(new EmptyBorder(15, 16, 14, 16));

        return panel;
    }

    private JPanel sectionHeader(String title, String subtitle) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(label(title, 10, ACCENT, Font.BOLD));
        panel.add(Box.createVerticalStrut(4));
        panel.add(label(subtitle, 12, MUTED, Font.PLAIN));

        return panel;
    }

    private JLabel label(
            String text,
            int size,
            Color color,
            int style
    ) {
        JLabel label = new JLabel(text);

        label.setForeground(color);
        label.setFont(new Font("Dialog", style, size));

        return label;
    }

    private JScrollPane scroll(JList<?> list) {
        JScrollPane pane = new JScrollPane(list);

        pane.setBorder(new LineBorder(BORDER, 1, true));
        pane.getViewport().setBackground(SURFACE_ALT);
        pane.setBackground(SURFACE_ALT);

        return pane;
    }

    private void styleList(JList<?> list) {
        list.setBackground(SURFACE_ALT);
        list.setForeground(TEXT);

        list.setSelectionBackground(new Color(47, 81, 111));
        list.setSelectionForeground(Color.WHITE);

        list.setFixedCellHeight(42);
        list.setBorder(new EmptyBorder(4, 4, 4, 4));
    }

   
    public void onManageQuestions(
            Consumer<List<QuizQuestion>> onSave,
            Supplier<List<QuizQuestion>> current
    ) {
        manageQuestionsButton.addActionListener(
                e -> showQuestionEditor(current.get(), onSave)
        );
    }

    private void showQuestionEditor(
            List<QuizQuestion> initial,
            Consumer<List<QuizQuestion>> onSave
    ) {
        Window owner = SwingUtilities.getWindowAncestor(this);

        JDialog dialog = new JDialog(
                owner,
                "Gerenciar perguntas",
                Dialog.ModalityType.APPLICATION_MODAL
        );

        dialog.setSize(760, 570);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(BG);

        JLabel heading = label(
                "SUAS PERGUNTAS",
                16,
                TEXT,
                Font.BOLD
        );

        JLabel hint = label(
                "Adicione perguntas e salve. Elas ficarão disponíveis nas próximas partidas.",
                11,
                MUTED,
                Font.PLAIN
        );

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        top.add(heading);
        top.add(Box.createVerticalStrut(5));
        top.add(hint);

        root.add(top, BorderLayout.NORTH);

        DefaultListModel<QuizQuestion> model =
                new DefaultListModel<>();

        initial.forEach(model::addElement);

        JList<QuizQuestion> list = new JList<>(model);

        list.setBackground(SURFACE_ALT);
        list.setForeground(TEXT);
        list.setSelectionBackground(new Color(47, 81, 111));

        list.setCellRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean selected,
                    boolean focus
            ) {
                JLabel component = (JLabel) super
                        .getListCellRendererComponent(
                                list,
                                value,
                                index,
                                selected,
                                focus
                        );

                component.setBackground(
                        selected
                                ? new Color(47, 81, 111)
                                : SURFACE_ALT
                );

                component.setForeground(TEXT);
                component.setBorder(
                        new EmptyBorder(7, 8, 7, 8)
                );

                return component;
            }
        });

        JScrollPane listScroll = new JScrollPane(list);

        listScroll.setPreferredSize(new Dimension(260, 300));
        listScroll.setBorder(new LineBorder(BORDER));

        JTextField question = new JTextField();

        JTextField[] options = {
                new JTextField(),
                new JTextField(),
                new JTextField(),
                new JTextField()
        };

        JComboBox<String> correct = new JComboBox<>(
                new String[]{"A", "B", "C", "D"}
        );

        JPanel fields = new JPanel(
                new GridLayout(0, 1, 5, 5)
        );

        fields.setOpaque(false);

        fields.add(label("Enunciado", 11, MUTED, Font.BOLD));
        fields.add(question);

        for (int i = 0; i < 4; i++) {
            char alternative = (char) ('A' + i);

            fields.add(
                    label(
                            "Alternativa " + alternative,
                            11,
                            MUTED,
                            Font.BOLD
                    )
            );

            fields.add(options[i]);
        }

        fields.add(
                label(
                        "Resposta correta",
                        11,
                        MUTED,
                        Font.BOLD
                )
        );

        fields.add(correct);

        fields.add(
                label(
                        "Explicação (opcional)",
                        11,
                        MUTED,
                        Font.BOLD
                )
        );

        JPanel editor = new JPanel(new BorderLayout(5, 5));
        editor.setOpaque(false);

        JScrollPane fieldsScroll = new JScrollPane(fields);

        fieldsScroll.setBorder(null);
        fieldsScroll.getViewport().setOpaque(false);
        fieldsScroll.setOpaque(false);

        editor.add(fieldsScroll, BorderLayout.CENTER);

        JButton add = new ActionButton(
                "Adicionar pergunta",
                ACCENT,
                new Color(8, 35, 39)
        );

        JButton update = new ActionButton(
                "Atualizar selecionada",
                BLUE,
                Color.WHITE
        );

        JButton remove = new ActionButton(
                "Excluir",
                SURFACE_ALT,
                TEXT
        );

        JPanel editActions = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 5, 0)
        );

        editActions.setOpaque(false);

        editActions.add(add);
        editActions.add(update);
        editActions.add(remove);

        editor.add(editActions, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                listScroll,
                editor
        );

        split.setResizeWeight(0.38);
        split.setBorder(null);

        root.add(split, BorderLayout.CENTER);

        Runnable clear = () -> {
            question.setText("");

            for (JTextField field : options) {
                field.setText("");
            }

            correct.setSelectedIndex(0);

            list.clearSelection();
        };

        Supplier<QuizQuestion> read = () -> new QuizQuestion(
                question.getText(),
                new String[]{
                        options[0].getText(),
                        options[1].getText(),
                        options[2].getText(),
                        options[3].getText()
                },
                correct.getSelectedIndex()
        );

        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            QuizQuestion selected = list.getSelectedValue();

            if (selected == null) {
                return;
            }

            question.setText(selected.getText());

            String[] alternatives = selected.getOptions();

            for (int i = 0; i < 4; i++) {
                options[i].setText(alternatives[i]);
            }

            correct.setSelectedIndex(selected.getCorrect());
        });

        add.addActionListener(e -> {
            try {
                model.addElement(read.get());
                clear.run();

            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                        dialog,
                        ex.getMessage(),
                        "Pergunta inválida",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });

        update.addActionListener(e -> {
            int index = list.getSelectedIndex();

            if (index < 0) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Selecione uma pergunta para atualizar."
                );

                return;
            }

            try {
                model.set(index, read.get());
                clear.run();

            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                        dialog,
                        ex.getMessage(),
                        "Pergunta inválida",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });

        remove.addActionListener(e -> {
            int index = list.getSelectedIndex();

            if (index >= 0) {
                model.remove(index);
            }

            clear.run();
        });

        JPanel bottom = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 0)
        );

        bottom.setOpaque(false);

        JButton cancel = new ActionButton(
                "Cancelar",
                SURFACE_ALT,
                TEXT
        );

        JButton save = new ActionButton(
                "Salvar perguntas",
                ACCENT,
                new Color(8, 35, 39)
        );

        bottom.add(cancel);
        bottom.add(save);

        root.add(bottom, BorderLayout.SOUTH);

        cancel.addActionListener(e -> dialog.dispose());

        save.addActionListener(e -> {
            if (model.isEmpty()) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Adicione pelo menos uma pergunta antes de salvar.",
                        "Quiz vazio",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            List<QuizQuestion> result = new ArrayList<>();

            for (int i = 0; i < model.size(); i++) {
                result.add(model.get(i));
            }

            onSave.accept(result);
            dialog.dispose();
        });

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }


    public void onHost(Consumer<Boolean> action) {
        hostButton.addActionListener(
                e -> action.accept(hostParticipates.isSelected())
        );
    }

    public void onRefresh(Runnable action) {
        refreshButton.addActionListener(
                e -> action.run()
        );
    }

    public void onJoin(Consumer<QuizRoom> action) {
        joinButton.addActionListener(e -> {
            QuizRoom room = roomsList.getSelectedValue();

            if (room != null) {
                action.accept(room);
            } else {
                setStatus(
                        "Selecione uma sala antes de tentar entrar."
                );
            }
        });
    }

    public void onStartGame(Runnable action) {
        startGameButton.addActionListener(
                e -> action.run()
        );
    }

    public void onAnswer(IntConsumer action) {
        for (int i = 0; i < answerButtons.length; i++) {
            final int option = i;

            answerButtons[i].addActionListener(e -> {
                action.accept(option);

                for (JButton button : answerButtons) {
                    button.setEnabled(false);
                }
            });
        }
    }

    public void showQuestion(
            String question,
            String[] options,
            int round,
            int total,
            int seconds,
            boolean canAnswer
    ) {
        gameCards.show(gameCard, "question");

        gameQuestion.setText(
                "Pergunta " + round + "/" + total + "  ·  " + question
        );

        gameTimer.setText(seconds + "s");

        gameFeedback.setText(
                canAnswer
                        ? "Escolha uma alternativa."
                        : "Você está acompanhando como administrador."
        );

        answerOptionsPanel.setVisible(canAnswer);

        for (int i = 0; i < answerButtons.length; i++) {
            String optionText = (
                    options != null && i < options.length
            ) ? options[i] : "";

            answerButtons[i].setText(
                    (char) ('A' + i) + "    " + optionText
            );

            answerButtons[i].setEnabled(canAnswer);
        }
    }

    public void setGameTimer(int seconds) {
        gameTimer.setText(seconds + "s");
    }

    public void setGameFeedback(String message) {
        gameFeedback.setText(message);
    }

    public void showRoundResult(
            int correct,
            String ranking
    ) {
        gameCards.show(gameCard, "question");

        answerOptionsPanel.setVisible(false);

        gameQuestion.setText("Resultado da rodada");
        gameTimer.setText("OK");

        gameFeedback.setText(
                "Resposta correta: "
                        + (char) ('A' + correct)
                        + "  |  "
                        + ranking
        );

        for (JButton button : answerButtons) {
            button.setEnabled(false);
        }
    }

    public void showFinalRanking(String[] names, int[] scores) {
        StringBuilder html = new StringBuilder(
                "<html><div style='line-height:1.7'>"
        );

        if (names == null || scores == null || names.length == 0) {
            html.append("Nenhuma pontuação registrada.");

        } else {
            int count = Math.min(names.length, scores.length);

            for (int i = 0; i < count; i++) {
                html.append("<b>")
                        .append(i + 1)
                        .append("º</b> &nbsp; ")
                        .append(escapeHtml(names[i]))
                        .append(" &nbsp; — &nbsp; <b>")
                        .append(scores[i])
                        .append(" ponto(s)</b><br>");
            }
        }

        html.append("</div></html>");

        rankingContent.setText(html.toString());

        gameCards.show(gameCard, "ranking");

        for (JButton button : answerButtons) {
            button.setEnabled(false);
        }
    }

    public void setInLobby(boolean inLobby, String roomName) {
        gameCard.setVisible(inLobby);

        if (inLobby) {
            gameCards.show(gameCard, "waiting");
            answerOptionsPanel.setVisible(false);

            lobbyTitle.setText(
                    roomName == null || roomName.isBlank()
                            ? "Lobby conectado"
                            : roomName
            );

        } else {
            gameCards.show(gameCard, "waiting");
        }

        revalidate();
        repaint();
    }

    public void setClientLobbyState(String roomName) {
        lobbyTitle.setText(
                roomName == null
                        ? "Lobby conectado"
                        : roomName
        );

        lobbyCount.setText("Conectado");

        participantsModel.clear();
        participantsModel.addElement("[REDE] Você");

        startGameButton.setEnabled(false);

        setInLobby(true, roomName);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "Jogador";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public void setGameAvailable(boolean hosting, boolean inGame) {
        startGameButton.setEnabled(hosting && !inGame);
    }

    public void setRooms(List<QuizRoom> rooms) {
        roomsModel.clear();
        rooms.forEach(roomsModel::addElement);
    }

    public void setLobbyState(
            boolean hosting,
            String roomName,
            boolean hostPlays,
            List<String> realPlayers,
            int maxPlayers
    ) {
        participantsModel.clear();

        if (!hosting) {
            setInLobby(false, "");

            lobbyTitle.setText("Nenhuma sala hospedada");
            lobbyCount.setText("0/" + maxPlayers);

            startGameButton.setEnabled(false);

            return;
        }

        setInLobby(true, roomName);

        lobbyTitle.setText(
                roomName
                        + (hostPlays
                        ? " · host participa"
                        : " · host administra")
        );

        int total = realPlayers.size() + (hostPlays ? 1 : 0);

        lobbyCount.setText(total + "/" + maxPlayers);

        participantsModel.addElement(
                hostPlays
                        ? "[HOST] Você (jogador)"
                        : "[HOST] Você (administrador)"
        );

        for (String name : realPlayers) {
            participantsModel.addElement("[REDE] " + name);
        }

        startGameButton.setEnabled(true);
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    private static class RoundedPanel extends JPanel {

        private final Color color;
        private final int radius;

        RoundedPanel(Color color, int radius) {
            this.color = color;
            this.radius = radius;

            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setColor(color);

            g.fill(
                    new RoundRectangle2D.Float(
                            0,
                            0,
                            getWidth(),
                            getHeight(),
                            radius,
                            radius
                    )
            );

            g.setColor(BORDER);

            g.draw(
                    new RoundRectangle2D.Float(
                            0.5f,
                            0.5f,
                            getWidth() - 1,
                            getHeight() - 1,
                            radius,
                            radius
                    )
            );

            g.dispose();

            super.paintComponent(graphics);
        }
    }

    private static class ActionButton extends JButton {

        private final Color fill;
        private final Color foreground;

        ActionButton(
                String text,
                Color fill,
                Color foreground
        ) {
            super(text);

            this.fill = fill;
            this.foreground = foreground;

            setForeground(foreground);
            setFont(new Font("Dialog", Font.BOLD, 11));

            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);

            setCursor(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );

            setBorder(new EmptyBorder(10, 14, 10, 14));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Color currentColor;

            if (!isEnabled()) {
                currentColor = new Color(55, 65, 82);

            } else if (getModel().isPressed()) {
                currentColor = fill.darker();

            } else if (getModel().isRollover()) {
                currentColor = fill.brighter();

            } else {
                currentColor = fill;
            }

            g.setColor(currentColor);

            g.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    12,
                    12
            );

            g.dispose();

            setForeground(isEnabled() ? foreground : MUTED);

            super.paintComponent(graphics);
        }
    }

    private static class AnswerButton extends JButton {

        private final Color fill;

        AnswerButton(String text, Color fill) {
            super(text);

            this.fill = fill;

            setForeground(Color.WHITE);
            setFont(new Font("Dialog", Font.BOLD, 13));

            setHorizontalAlignment(SwingConstants.LEFT);

            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);

            setCursor(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );

            setBorder(new EmptyBorder(14, 16, 14, 16));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Color currentColor = isEnabled()
                    ? (getModel().isRollover()
                    ? fill.brighter()
                    : fill)
                    : new Color(43, 55, 73);

            g.setColor(currentColor);

            g.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    13,
                    13
            );

            g.dispose();

            setForeground(isEnabled() ? Color.WHITE : MUTED);

            super.paintComponent(graphics);
        }
    }

    private class RoomRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focus
        ) {
            JLabel component = (JLabel) super
                    .getListCellRendererComponent(
                            list,
                            value,
                            index,
                            selected,
                            focus
                    );

            component.setOpaque(true);

            component.setBackground(
                    selected
                            ? new Color(47, 81, 111)
                            : SURFACE_ALT
            );

            component.setForeground(TEXT);
            component.setBorder(new EmptyBorder(8, 10, 8, 10));
            component.setFont(new Font("Dialog", Font.PLAIN, 12));

            return component;
        }
    }

    private class ParticipantRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focus
        ) {
            JLabel component = (JLabel) super
                    .getListCellRendererComponent(
                            list,
                            value,
                            index,
                            selected,
                            focus
                    );

            component.setOpaque(true);

            component.setBackground(
                    selected
                            ? new Color(47, 81, 111)
                            : SURFACE_ALT
            );

            component.setForeground(TEXT);
            component.setBorder(new EmptyBorder(8, 10, 8, 10));
            component.setFont(new Font("Dialog", Font.PLAIN, 12));

            return component;
        }
    }
}
