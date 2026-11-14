package pitiupi.quiz.net;
import pitiupi.models.Messages.Message;

// Resposta jogador -> host
public class QuizAnswerMessage extends Message {
    private String gameId, roundId, playerName, playerInstanceId;
    private int selectedOption;
    private long answeredAt;
    
    public QuizAnswerMessage() {}
    
    public QuizAnswerMessage(String gameId, String roundId, String playerName, String playerInstanceId, int selectedOption, long answeredAt)
    {
        this.gameId=gameId;
        this.roundId=roundId;
        this.playerName=playerName;
        this.playerInstanceId=playerInstanceId;
        this.selectedOption=selectedOption;
        this.answeredAt=answeredAt;
    }

    public String getGameId() {return gameId;} 
    
    public String getRoundId() {return roundId;} 
    
    public String getPlayerName() {return playerName;} 
    
    public String getPlayerInstanceId() {return playerInstanceId;} 
    
    public int getSelectedOption() {return selectedOption;} 
    
    public long getAnsweredAt() {return answeredAt;}
}
