package atomniyivan.archery_game.server;

import atomniyivan.archery_game.client.GameClient;
import atomniyivan.archery_game.models.PlayMapInfo;
import atomniyivan.archery_game.models.Player;
import atomniyivan.archery_game.states.Action;
import atomniyivan.archery_game.states.GameState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static atomniyivan.archery_game.client.GameClient.gson;

public class ServerHandler extends Thread {
    GameClient gameClient;
    private final Socket clientSocket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public ServerHandler(GameClient gameClient, Socket socket,
                         DataInputStream dis, DataOutputStream dos) {
        this.gameClient = gameClient;
        clientSocket = socket;
        in = dis;
        out = dos;
        setDaemon(true);
        start();
    }

    @Override
    public void run() {
        try {
            requestGameInfo();
            handleMessages();
        } catch (IOException e) {
            throw new RuntimeException();
        } finally {
            closeHandler();
        }
    }

    private void requestGameInfo() throws IOException {
        String jsonInfo = in.readUTF();
        PlayMapInfo playMapInfo = gson.fromJson(jsonInfo, PlayMapInfo.class);
        gameClient.addPlayersToGame(playMapInfo);
    }

    private void handleMessages() throws IOException {
        while (true) {
            String msg = in.readUTF();
            Action action = gson.fromJson(msg, Action.class);
            switch (action.type()) {
                case NEW_PLAYER -> gameClient.addPlayer(gson.fromJson(action.info(), Player.class));
                case WANT_TO_START -> gameClient.setPlayerWantToStart(action.info());
                case STATE -> gameClient.setState(gson.fromJson(action.info(), GameState.class));
                case UPDATE -> gameClient.updateGameInfo(gson.fromJson(action.info(), PlayMapInfo.class));
                case WANT_TO_PAUSE -> gameClient.updatePlayerWantToPause(action.info());
                case WINNER -> gameClient.showWinner(gson.fromJson(action.info(), Player.class));
                case RESET -> gameClient.resetGameInfo(gson.fromJson(action.info(), PlayMapInfo.class));
                case REMOVE_PLAYER -> gameClient.removePlayer(action.info());
                case STOP -> gameClient.showStop();
            }
        }
    }

    private void closeHandler() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
