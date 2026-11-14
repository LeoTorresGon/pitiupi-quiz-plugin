package pitiupi.quiz.net;

import pitiupi.models.Messages.Message;

// Resposta pedido entrada host -> jogador
public class QuizJoinResponseMessage extends Message {
    private String gameId;
    private boolean accepted;
    private String reason;
    private String hostName;

    public QuizJoinResponseMessage() { }

    public QuizJoinResponseMessage(String gameId, boolean accepted, String reason, String hostName) {
        this.gameId = gameId;
        this.accepted = accepted;
        this.reason = reason;
        this.hostName = hostName;
    }
    public String getGameId() { return gameId; }

    public boolean isAccepted() { return accepted; }

    public String getReason() { return reason; }

    public String getHostName() { return hostName; }
}
