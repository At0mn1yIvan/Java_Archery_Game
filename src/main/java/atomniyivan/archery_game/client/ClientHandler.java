package atomniyivan.archery_game.client;

import atomniyivan.archery_game.states.Action;
import atomniyivan.archery_game.models.Player;
import atomniyivan.archery_game.server.GameServer;

import java.io.*;
import java.net.Socket;

import static atomniyivan.archery_game.server.GameServer.gson;

public class ClientHandler extends Thread {
    private final GameServer server;
    private final Socket clientSocket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private Player player;

    public ClientHandler(GameServer server, Socket socket) throws IOException {
        this.server = server;
        clientSocket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        setDaemon(true);
        start();
    }

    @Override
    public void run() {
        try {
            checkPlayersProperties();
            handleMessages();
        } catch (IOException e) {
            stopConnection();
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    private void checkPlayersProperties() throws IOException {
        String nickname = in.readUTF();
        while (server.isNicknameExists(nickname)) {
            out.writeUTF(nickname + " уже присутсвует.");
            nickname = in.readUTF();
        }
        while (nickname.isEmpty()) {
            out.writeUTF("Имя не может быть пустой строкой.");
            nickname = in.readUTF();
        }
        while (server.isGameStarted()) {
            out.writeUTF("Игра уже началась.");
            nickname = in.readUTF();
        }
        while (!server.correctPlayersAmount()) {
            out.writeUTF("MAX_PLAYERS_REACHED");
        }
        out.writeUTF("OK");
        server.addNewPlayer(nickname, this);
    }

    private void handleMessages() throws IOException {
        while (true) {
            String msg = in.readUTF();
            Action.Type actionType = gson.fromJson(msg, Action.Type.class);
            switch (actionType) {
                case WANT_TO_START:
                    player.wantToStart = true;
                    server.sendWantToStartAction(this);
                    server.startGame();
                    break;
                case SHOOT:
                    player.isShooting = true;
                    ++player.shots;
                    break;
                case WANT_TO_PAUSE:
                    player.wantToPause = !player.wantToPause;
                    server.sendWantToPauseAction(this);
                    server.pauseGame();
                    break;
            }
        }
    }

    private void stopConnection() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            server.removePlayer(this);
        }
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            stopConnection();
        }
    }
}
