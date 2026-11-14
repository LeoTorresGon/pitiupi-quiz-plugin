package pitiupi.quiz.model;

import java.net.InetAddress;

public record QuizRoom(String gameId, String roomName, String hostName,
                       InetAddress hostAddress, int currentPlayers, int maxPlayers,
                       boolean hostParticipates, long lastSeen) {
    public boolean isExpired(long now, long ttlMillis) {
        return now - lastSeen > ttlMillis;
    }

    @Override public String toString() {
        String hostMode = hostParticipates ? "host joga" : "host administra";
        return roomName + " — " + hostName + " (" + currentPlayers + "/" + maxPlayers
                + ", " + hostMode + ")";
    }
}
