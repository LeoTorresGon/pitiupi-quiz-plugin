package pitiupi.quiz.net;
import pitiupi.models.Messages.Message;

// Resultado da rodada host -> jogador
public class QuizRoundResultMessage extends Message {
    private String gameId, roundId, explanation;
    private int correctOption, roundNumber;
    private String[] rankingNames;
    private int[] rankingScores;

    public QuizRoundResultMessage() {}

    public QuizRoundResultMessage(String gameId, String roundId, int correctOption, int roundNumber, String[] names, int[] scores){
        this.gameId=gameId;
        this.roundId=roundId;
        this.correctOption=correctOption;
        this.roundNumber=roundNumber;
        this.rankingNames=names;
        this.rankingScores=scores;
    }

    public String getGameId(){return gameId;} public String getRoundId(){return roundId;} public int getCorrectOption(){return correctOption;} public int getRoundNumber(){return roundNumber;} public String getExplanation(){return explanation;} public String[] getRankingNames(){return rankingNames;} public int[] getRankingScores(){return rankingScores;}
}
