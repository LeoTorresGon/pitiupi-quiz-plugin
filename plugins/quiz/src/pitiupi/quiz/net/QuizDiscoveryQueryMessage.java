package pitiupi.quiz.net;

import pitiupi.models.Messages.Message;

// Pergunta se tem sala aberta jogador -> multicast
public class QuizDiscoveryQueryMessage extends Message {
    private long requestedAt;
    
    public QuizDiscoveryQueryMessage() { }

    public QuizDiscoveryQueryMessage(long requestedAt) { 
        this.requestedAt = requestedAt; 
    }

    public long getRequestedAt() { return requestedAt; }
}
