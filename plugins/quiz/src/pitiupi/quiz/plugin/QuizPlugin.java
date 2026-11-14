package pitiupi.quiz.plugin;

import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.messaging.MessageDispatcher;
import pitiupi.application.messaging.SendTarget;
import pitiupi.application.messaging.TransportMode;
import pitiupi.application.presence.UserPeerView;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;
import pitiupi.quiz.model.QuizRoom;
import pitiupi.quiz.model.QuizQuestion;
import pitiupi.quiz.model.QuizQuestionStore;
import pitiupi.quiz.net.*;
import pitiupi.quiz.ui.QuizPanel;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuizPlugin extends Plugin {
    private static final long ROOM_TTL_MS = 10_000;
    private static final int MAX_PLAYERS = 8;
    private ConnectionManager connectionManager;
    private PluginWorkspace workspace;
    private QuizPanel panel;
    private final Map<String, QuizRoom> discoveredRooms = new ConcurrentHashMap<>();
    private final Set<String> acceptedPlayers = ConcurrentHashMap.newKeySet();
    private String hostedGameId;
    private String hostedRoomName;
    private boolean hostParticipates;
    private Timer announceTimer;
    private Timer roundTimer;
    private Timer nextRoundTimer;
    private Timer countdownTimer;
    private Timer answerRetryTimer;
    private QuizAnswerMessage pendingAnswer;
    private int pendingAnswerRetries;
    private static final int ANSWER_RETRY_INTERVAL_MS = 500;
    private static final int MAX_ANSWER_RETRIES = 3;
    private QuizRoom joinedRoom;
    private boolean gameRunning;
    private int roundIndex;
    private int secondsLeft;
    private String activeRoundId;
    private long roundStartedAt;
    private final Map<String, Integer> scores = new LinkedHashMap<>();
    private final Set<String> answeredPlayers = new HashSet<>();
    private final List<QuizQuestion> questions = new ArrayList<>();
    private final QuizQuestionStore questionStore = new QuizQuestionStore();
    private int receivedQuestionCount;

    @Override public String getName() { return "Quiz P2P"; }
    @Override public String getDisplayName() { return "Quiz multiplayer"; }
    @Override public String getAuthor() { return "Projeto acadêmico"; }
    @Override public String getVersion() { return "0.1"; }
    @Override public String getIdentifier() { return "pitiupi.quiz"; }

    @Override public void createPlugin(ConnectionManager cm, PluginWorkspace ws) {
        connectionManager = cm;
        workspace = ws;
        try { questions.addAll(questionStore.load()); }
        catch (Exception ex) { System.err.println("Não foi possível carregar as perguntas do quiz: " + ex.getMessage()); }
        panel = new QuizPanel();
        panel.onManageQuestions(this::saveQuestions, () -> new ArrayList<>(questions));
        panel.setStatus(questions.size() + " pergunta(s) carregadas do cache. Use Perguntas para editar o quiz.");
        panel.onHost(this::createRoom);
        panel.onRefresh(this::discoverRooms);
        panel.onJoin(this::requestJoin);
        panel.onStartGame(this::startGame);
        panel.onAnswer(this::handleAnswer);
        bindShellWorkspaceContent(panel);
    }

    @Override public List<Class<? extends Message>> getMessages() {
        return List.of(QuizDiscoveryMessage.class, QuizDiscoveryQueryMessage.class,
                QuizJoinRequestMessage.class, QuizJoinResponseMessage.class,
                QuizQuestionMessage.class, QuizAnswerMessage.class, QuizAnswerAckMessage.class, QuizRoundResultMessage.class);
    }

    @Override public void receiveMessage(Message message) {
        if (message instanceof QuizDiscoveryMessage msg) {
            receiveAnnouncement(msg);
        } else if (message instanceof QuizDiscoveryQueryMessage) {
            if (hostedGameId != null) announceRoom();
        } else if (message instanceof QuizJoinRequestMessage msg) {
            receiveJoinRequest(msg);
        } else if (message instanceof QuizQuestionMessage msg) {
            receiveQuestion(msg);
        } else if (message instanceof QuizAnswerMessage msg) {
            receiveAnswer(msg);
        } else if (message instanceof QuizAnswerAckMessage msg) {
            receiveAnswerAck(msg);
        } else if (message instanceof QuizRoundResultMessage msg) {
            receiveRoundResult(msg);
        } else if (message instanceof QuizJoinResponseMessage msg) {
            SwingUtilities.invokeLater(() -> {
                if (msg.isAccepted()) {
                    panel.setClientLobbyState(joinedRoom != null ? joinedRoom.roomName() : msg.getHostName());
                    panel.setStatus("Entrada aceita por " + msg.getHostName() + ". Aguardando início da partida.");
                } else {
                    joinedRoom = null;
                    panel.setInLobby(false, "");
                    panel.setStatus("Entrada recusada: " + msg.getReason());
                }
            });
        }
    }

    private void saveQuestions(List<QuizQuestion> updated) {
        try {
            questionStore.save(updated);
            questions.clear(); questions.addAll(updated);
            panel.setStatus(questions.size() + " pergunta(s) salvas no cache do Pitiupi.");
        } catch (Exception ex) {
            panel.setStatus("Erro ao salvar perguntas: " + ex.getMessage());
        }
    }

    private void createRoom(boolean hostParticipates) {
        if (questions.isEmpty()) {
            panel.setStatus("Crie e salve pelo menos uma pergunta antes de abrir uma sala.");
            return;
        }
        if (hostedGameId != null) {
            panel.setStatus("Você já está anunciando uma sala.");
            return;
        }
        hostedGameId = UUID.randomUUID().toString();
        hostedRoomName = connectionManager.getUserName() + " — Quiz";
        this.hostParticipates = hostParticipates;
        acceptedPlayers.clear();
        refreshLobby();
        announceRoom();
        announceTimer = new Timer(3000, e -> { pruneRooms(); announceRoom(); });
        announceTimer.start();
        refreshLobby();
        panel.setStatus("Sala criada com sucesso! Está aberta e aguardando participantes. Código: "
                + hostedGameId.substring(0, 8));
    }

    private void discoverRooms() {
        pruneRooms();
        MessageDispatcher.send(connectionManager, new QuizDiscoveryQueryMessage(System.currentTimeMillis()),
                SendTarget.broadcastAll(), TransportMode.MULTICAST_UDP);
        panel.setRooms(new ArrayList<>(discoveredRooms.values()));
        panel.setStatus("Procurando salas abertas na rede...");
    }

    private void announceRoom() {
        if (hostedGameId == null) return;
        QuizDiscoveryMessage message = new QuizDiscoveryMessage(hostedGameId, hostedRoomName,
                connectionManager.getUserName(), connectionManager.getHeartbeatManager().getInstanceId(),
                acceptedPlayers.size() + (hostParticipates ? 1 : 0), MAX_PLAYERS,
                hostParticipates, System.currentTimeMillis());
        MessageDispatcher.send(connectionManager, message, SendTarget.broadcastAll(), TransportMode.MULTICAST_UDP);
    }

    private void receiveAnnouncement(QuizDiscoveryMessage msg) {
        if (msg.getGameId() == null || msg.getHostName() == null || msg.getGameId().equals(hostedGameId)) return;
        InetAddress address = findPeerAddress(msg.getHostName(), msg.getHostInstanceId());
        if (address == null) return;
        QuizRoom room = new QuizRoom(msg.getGameId(), msg.getRoomName(), msg.getHostName(), address,
                msg.getCurrentPlayers(), msg.getMaxPlayers(), msg.isHostParticipates(),
                System.currentTimeMillis());
        discoveredRooms.put(msg.getGameId(), room);
        SwingUtilities.invokeLater(() -> { pruneRooms(); panel.setRooms(new ArrayList<>(discoveredRooms.values())); });
    }

    private void requestJoin(QuizRoom room) {
        joinedRoom = room;
        MessageDispatcher.send(connectionManager,
                new QuizJoinRequestMessage(room.gameId(), connectionManager.getUserName(),
                        connectionManager.getHeartbeatManager().getInstanceId()),
                SendTarget.singlePeer(room.hostAddress()), TransportMode.UNICAST_UDP);
        panel.setStatus("Solicitação enviada para " + room.hostName() + ". Aguardando confirmação...");
    }

    private void receiveJoinRequest(QuizJoinRequestMessage msg) {
        if (hostedGameId == null || !hostedGameId.equals(msg.getGameId())) return;
        int occupiedSlots = acceptedPlayers.size() + (hostParticipates ? 1 : 0);
        boolean accepted = !gameRunning && occupiedSlots < MAX_PLAYERS;
        if (accepted) acceptedPlayers.add(msg.getPlayerName());
        InetAddress playerAddress = findPeerAddress(msg.getPlayerName(), msg.getPlayerInstanceId());
        if (playerAddress != null) {
            MessageDispatcher.send(connectionManager,
                    new QuizJoinResponseMessage(hostedGameId, accepted,
                            accepted ? "" : (gameRunning ? "Partida já iniciada" : "Sala cheia"), connectionManager.getUserName()),
                    SendTarget.singlePeer(playerAddress), TransportMode.UNICAST_UDP);
        }
        SwingUtilities.invokeLater(() -> {
            if (accepted) panel.setStatus(msg.getPlayerName() + " entrou na sala.");
            else panel.setStatus("A solicitação de " + msg.getPlayerName() + " foi recusada: sala cheia.");
            refreshLobby();
        });
        announceRoom();
    }

    private void startGame() {
        if (hostedGameId == null || gameRunning) return;
        int players = acceptedPlayers.size() + (hostParticipates ? 1 : 0);
        if (players == 0) { panel.setStatus("Adicione participantes ou habilite a participação do host antes de iniciar."); return; }
        gameRunning = true; roundIndex = 0; scores.clear(); answeredPlayers.clear();
        for (String name : acceptedPlayers) scores.put(name, 0);
        if (hostParticipates) scores.put(connectionManager.getUserName(), 0);
        panel.setGameAvailable(true, true);
        panel.setStatus("Partida iniciada!");
        beginRound();
    }

    private void beginRound() {
        if (!gameRunning) return;
        if (roundIndex >= questions.size()) { finishGame(); return; }
        QuizQuestion q = questions.get(roundIndex);
        activeRoundId = UUID.randomUUID().toString(); roundStartedAt = System.currentTimeMillis();
        answeredPlayers.clear(); secondsLeft = 20;
        QuizQuestionMessage message = new QuizQuestionMessage(hostedGameId, activeRoundId,
                connectionManager.getHeartbeatManager().getInstanceId(), q.getText(), q.getOptions(),
                roundIndex + 1, questions.size(), 20);
        MessageDispatcher.send(connectionManager, message, SendTarget.broadcastAll(), TransportMode.MULTICAST_UDP);
        panel.showQuestion(q.getText(), q.getOptions(), roundIndex + 1, questions.size(), 20, hostParticipates);
        if (hostParticipates) panel.setGameFeedback("Sua vez de responder!");
        else panel.setGameFeedback("Você está administrando esta partida.");
        if (countdownTimer != null) countdownTimer.stop();
        countdownTimer = new Timer(1000, e -> {
            secondsLeft--; panel.setGameTimer(Math.max(0, secondsLeft));
            if (secondsLeft <= 0) endRound();
        });
        countdownTimer.start();
    }

    private void handleAnswer(int option) {
        if (hostedGameId != null) { submitLocalAnswer(option); return; }
        if (joinedRoom == null || activeRoundId == null || pendingAnswer != null) return;
        pendingAnswer = new QuizAnswerMessage(joinedRoom.gameId(), activeRoundId,
                connectionManager.getUserName(), connectionManager.getHeartbeatManager().getInstanceId(),
                option, System.currentTimeMillis());
        pendingAnswerRetries = 0;
        sendPendingAnswer();
        if (answerRetryTimer != null) answerRetryTimer.stop();
        answerRetryTimer = new Timer(ANSWER_RETRY_INTERVAL_MS, e -> {
            if (pendingAnswer == null) { ((Timer)e.getSource()).stop(); return; }
            if (pendingAnswerRetries >= MAX_ANSWER_RETRIES) {
                ((Timer)e.getSource()).stop();
                panel.setGameFeedback("Resposta enviada, mas sem confirmação do host. Aguarde o resultado.");
                return;
            }
            pendingAnswerRetries++;
            sendPendingAnswer();
            panel.setGameFeedback("Aguardando confirmação do host... (tentativa "
                    + (pendingAnswerRetries + 1) + "/" + (MAX_ANSWER_RETRIES + 1) + ")");
        });
        answerRetryTimer.start();
    }

    private void submitLocalAnswer(int option) {
        if (!gameRunning || !hostParticipates || activeRoundId == null) return;
        acceptAnswer(connectionManager.getUserName(), option, System.currentTimeMillis());
        panel.setGameFeedback("Resposta enviada: " + (char)('A' + option));
    }

    private void receiveQuestion(QuizQuestionMessage msg) {
        receivedQuestionCount = msg.getTotalRounds();
        if (msg.getGameId() == null || msg.getGameId().equals(hostedGameId)) return;
        if (joinedRoom == null || !joinedRoom.gameId().equals(msg.getGameId())) return;
        InetAddress hostAddress = joinedRoom.hostAddress();
        SwingUtilities.invokeLater(() -> {
            clearPendingAnswer();
            gameRunning = true; activeRoundId = msg.getRoundId();
            panel.showQuestion(msg.getQuestion(), msg.getOptions(), msg.getRoundNumber(), msg.getTotalRounds(), msg.getDurationSeconds(), true);
            panel.setStatus("Partida em andamento — responda antes do tempo acabar.");
            if (countdownTimer != null) countdownTimer.stop();
            final int[] remaining = {msg.getDurationSeconds()};
            countdownTimer = new Timer(1000, e -> { remaining[0]--; panel.setGameTimer(Math.max(0, remaining[0])); if (remaining[0] <= 0) { ((Timer)e.getSource()).stop(); panel.setGameFeedback("Tempo esgotado."); } });
            countdownTimer.start();
        });
    }

    private void receiveAnswer(QuizAnswerMessage msg) {
        SwingUtilities.invokeLater(() -> {
            if (!gameRunning || hostedGameId == null || !hostedGameId.equals(msg.getGameId())
                    || !Objects.equals(activeRoundId, msg.getRoundId())
                    || msg.getSelectedOption() < 0 || msg.getSelectedOption() > 3
                    || msg.getPlayerName() == null || !scores.containsKey(msg.getPlayerName())) return;

            // Responde inclusive a retransmissões: o cliente precisa parar de reenviar,
            // mas a resposta só pode ser contabilizada uma vez por identidade/rodada.
            InetAddress playerAddress = findPeerAddress(msg.getPlayerName(), msg.getPlayerInstanceId());
            if (playerAddress != null && msg.getPlayerInstanceId() != null) {
                MessageDispatcher.send(connectionManager,
                        new QuizAnswerAckMessage(hostedGameId, activeRoundId, msg.getPlayerInstanceId()),
                        SendTarget.singlePeer(playerAddress), TransportMode.UNICAST_UDP);
            }
            String answerKey = msg.getPlayerInstanceId() == null || msg.getPlayerInstanceId().isBlank()
                    ? msg.getPlayerName() : msg.getPlayerInstanceId();
            acceptAnswer(msg.getPlayerName(), answerKey, msg.getSelectedOption(), msg.getAnsweredAt());
        });
    }

    private void receiveAnswerAck(QuizAnswerAckMessage msg) {
        SwingUtilities.invokeLater(() -> {
            if (pendingAnswer == null || joinedRoom == null) return;
            if (!Objects.equals(pendingAnswer.getGameId(), msg.getGameId())
                    || !Objects.equals(pendingAnswer.getRoundId(), msg.getRoundId())
                    || !Objects.equals(pendingAnswer.getPlayerInstanceId(), msg.getPlayerInstanceId())) return;
            clearPendingAnswer();
            panel.setGameFeedback("Resposta confirmada pelo host!");
        });
    }

    private void sendPendingAnswer() {
        if (pendingAnswer == null || joinedRoom == null) return;
        MessageDispatcher.send(connectionManager, pendingAnswer,
                SendTarget.singlePeer(joinedRoom.hostAddress()), TransportMode.UNICAST_UDP);
    }

    private void clearPendingAnswer() {
        if (answerRetryTimer != null) { answerRetryTimer.stop(); answerRetryTimer = null; }
        pendingAnswer = null;
        pendingAnswerRetries = 0;
    }

    private void acceptAnswer(String player, int option, long answeredAt) {
        acceptAnswer(player, player, option, answeredAt);
    }

    private void acceptAnswer(String player, String answerKey, int option, long answeredAt) {
        if (!gameRunning || activeRoundId == null || option < 0 || option > 3 || !scores.containsKey(player)) return;
        if (!answeredPlayers.add(answerKey)) return;
        QuizQuestion q = questions.get(roundIndex);
        if (option == q.getCorrect()) scores.put(player, scores.get(player) + 1);
        if (answeredPlayers.size() >= scores.size()) endRound();
    }

    private void endRound() {
        if (!gameRunning || activeRoundId == null) return;
        if (countdownTimer != null) countdownTimer.stop();
        QuizQuestion q = questions.get(roundIndex);
        List<Map.Entry<String,Integer>> ranking = new ArrayList<>(scores.entrySet());
        ranking.sort((a,b) -> Integer.compare(b.getValue(), a.getValue()));
        String[] names = ranking.stream().map(Map.Entry::getKey).toArray(String[]::new);
        int[] points = ranking.stream().mapToInt(Map.Entry::getValue).toArray();
        String summary = String.join(", ", java.util.stream.IntStream.range(0, names.length).mapToObj(i -> names[i] + ": " + points[i]).toList());
        QuizRoundResultMessage result = new QuizRoundResultMessage(hostedGameId, activeRoundId, q.getCorrect(), roundIndex + 1, names, points);
        MessageDispatcher.send(connectionManager, result, SendTarget.broadcastAll(), TransportMode.TCP);
        panel.showRoundResult(q.getCorrect(), summary);
        activeRoundId = null; roundIndex++;
        if (nextRoundTimer != null) nextRoundTimer.stop();
        nextRoundTimer = new Timer(5000, e -> { ((Timer)e.getSource()).stop(); beginRound(); });
        nextRoundTimer.setRepeats(false); nextRoundTimer.start();
    }

    private void receiveRoundResult(QuizRoundResultMessage msg) {
        if (joinedRoom == null || !joinedRoom.gameId().equals(msg.getGameId())) return;
        SwingUtilities.invokeLater(() -> {
            clearPendingAnswer();
            String[] names = msg.getRankingNames(); int[] points = msg.getRankingScores();
            List<String> lines = new ArrayList<>();
            if (names != null && points != null) for (int i=0;i<Math.min(names.length,points.length);i++) lines.add(names[i] + ": " + points[i]);
            panel.showRoundResult(msg.getCorrectOption(), String.join(", ", lines));
            if (msg.getRoundNumber() >= receivedQuestionCount) {
                gameRunning = false;
                activeRoundId = null;
                panel.showFinalRanking(names, points);
                panel.setStatus("Quiz finalizado! Ranking final recebido.");
            } else {
                panel.setStatus("Resultado da rodada " + msg.getRoundNumber() + " recebido. Próxima pergunta em instantes.");
            }
        });
    }

    private void finishGame() {
        gameRunning = false;
        activeRoundId = null;
        if (countdownTimer != null) countdownTimer.stop();
        if (nextRoundTimer != null) nextRoundTimer.stop();
        clearPendingAnswer();

        List<Map.Entry<String, Integer>> ranking = new ArrayList<>(scores.entrySet());
        ranking.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        String[] names = ranking.stream().map(Map.Entry::getKey).toArray(String[]::new);
        int[] points = ranking.stream().mapToInt(Map.Entry::getValue).toArray();
        panel.showFinalRanking(names, points);
        panel.setGameAvailable(hostedGameId != null, false);
        panel.setStatus("Quiz finalizado! Ranking final exibido.");
    }

    private void refreshLobby() {
        if (panel == null) return;
        panel.setLobbyState(hostedGameId != null, hostedRoomName, hostParticipates,
                new ArrayList<>(acceptedPlayers), MAX_PLAYERS);
    }

    private InetAddress findPeerAddress(String displayName, String instanceId) {
        for (UserPeerView peer : connectionManager.getPresenceService().listPeerViews()) {
            if (!peer.isSelf() && !peer.isBroadcastTarget()
                    && (instanceId != null && instanceId.equals(peer.getInstanceId())
                    || instanceId == null && displayName.equals(peer.getDisplayName()))) {
                return peer.getInetAddress().orElse(null);
            }
        }
        return null;
    }

    private void pruneRooms() {
        long now = System.currentTimeMillis();
        discoveredRooms.values().removeIf(room -> room.isExpired(now, ROOM_TTL_MS));
    }

    @Override public void onShellTabClosed() {
        gameRunning = false; joinedRoom = null; activeRoundId = null;
        clearPendingAnswer();
        if (countdownTimer != null) countdownTimer.stop();
        if (nextRoundTimer != null) nextRoundTimer.stop();
        if (announceTimer != null) { announceTimer.stop(); announceTimer = null; }
        hostedGameId = null;
        hostedRoomName = null;
        hostParticipates = false;
        acceptedPlayers.clear();
        if (panel != null) SwingUtilities.invokeLater(() -> {
            panel.setLobbyState(false, "", true, List.of(), MAX_PLAYERS);
            panel.setStatus("Lobby encerrado.");
        });
    }
}
