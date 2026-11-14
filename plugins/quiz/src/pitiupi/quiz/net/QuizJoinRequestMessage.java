package pitiupi.quiz.net;

import pitiupi.models.Messages.Message;

// Pedido entrar na sala jogador -> host
public class QuizJoinRequestMessage extends Message {
    private String gameId;
    private String playerName;
    private String playerInstanceId;

    public QuizJoinRequestMessage() { }

    public QuizJoinRequestMessage(String gameId, String playerName, String playerInstanceId) {
        this.gameId = gameId;
        this.playerName = playerName;
        this.playerInstanceId = playerInstanceId;
    }

    public String getGameId() { return gameId; }

    public String getPlayerName() { return playerName; }

    public String getPlayerInstanceId() { return playerInstanceId; }
}
