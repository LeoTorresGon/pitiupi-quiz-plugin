package pitiupi.quiz.net;

import pitiupi.models.Messages.Message;

// Ack host -> jogador
public class QuizAnswerAckMessage extends Message {
    private String gameId;
    private String roundId;
    private String playerInstanceId;

    public QuizAnswerAckMessage() {}

    public QuizAnswerAckMessage(String gameId, String roundId, String playerInstanceId) {
        this.gameId = gameId;
        this.roundId = roundId;
        this.playerInstanceId = playerInstanceId;
    }

    public String getGameId() { return gameId; }
    public String getRoundId() { return roundId; }
    public String getPlayerInstanceId() { return playerInstanceId; }
}
