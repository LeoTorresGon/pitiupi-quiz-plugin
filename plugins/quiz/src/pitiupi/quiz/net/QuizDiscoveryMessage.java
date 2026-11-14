package pitiupi.quiz.net;

import pitiupi.models.Messages.Message;

// Anuncia sala aberta host -> multicast
public class QuizDiscoveryMessage extends Message {
    private String gameId;
    private String roomName;
    private String hostName;
    private String hostInstanceId;
    private int currentPlayers;
    private int maxPlayers;
    private boolean hostParticipates;
    private long announcedAt;

    public QuizDiscoveryMessage() { }

    public QuizDiscoveryMessage(String gameId, String roomName, String hostName, String hostInstanceId,
                                int currentPlayers, int maxPlayers, boolean hostParticipates,
                                long announcedAt) {
        this.gameId = gameId;
        this.roomName = roomName;
        this.hostName = hostName;
        this.hostInstanceId = hostInstanceId;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.hostParticipates = hostParticipates;
        this.announcedAt = announcedAt;
    }

    public String getGameId() { return gameId; }
    public String getRoomName() { return roomName; }
    public String getHostName() { return hostName; }
    public String getHostInstanceId() { return hostInstanceId; }
    public int getCurrentPlayers() { return currentPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public boolean isHostParticipates() { return hostParticipates; }
    public long getAnnouncedAt() { return announcedAt; }
}
