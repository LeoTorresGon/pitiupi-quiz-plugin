package pitiupi.quiz.net;
import pitiupi.models.Messages.Message;

// Questão host -> jogador
public class QuizQuestionMessage extends Message {

    private String gameId, roundId, hostInstanceId, question; private String[] options; private int roundNumber, totalRounds, durationSeconds;

    public QuizQuestionMessage() {}

    public QuizQuestionMessage(String gameId, String roundId, String hostInstanceId, String question, String[] options, int roundNumber, int totalRounds, int durationSeconds){
        this.gameId=gameId;
        this.roundId=roundId;
        this.hostInstanceId=hostInstanceId;
        this.question=question;
        this.options=options;
        this.roundNumber=roundNumber;
        this.totalRounds=totalRounds;
        this.durationSeconds=durationSeconds;
    }
 
    public String getGameId(){return gameId;} public String getRoundId(){return roundId;} public String getHostInstanceId(){return hostInstanceId;} public String getQuestion(){return question;} public String[] getOptions(){return options;} public int getRoundNumber(){return roundNumber;} public int getTotalRounds(){return totalRounds;} public int getDurationSeconds(){return durationSeconds;}
}
