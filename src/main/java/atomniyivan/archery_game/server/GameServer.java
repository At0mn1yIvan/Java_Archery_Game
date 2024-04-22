package atomniyivan.archery_game.server;

import atomniyivan.archery_game.client.ClientHandler;
import atomniyivan.archery_game.hibernate.Database;
import atomniyivan.archery_game.models.ArrowInfo;
import atomniyivan.archery_game.models.PlayMapInfo;
import atomniyivan.archery_game.models.Player;
import atomniyivan.archery_game.models.TargetModel;
import atomniyivan.archery_game.states.Action;
import atomniyivan.archery_game.states.GameState;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.sqrt;

public class GameServer {
    public static final Gson gson = new Gson();
    private static final Random random = new Random();
    private static final String[] colors = {"#dc8a78", "#dd7878", "#ea76cb", "#8839ef"};
    private static final double height = 439;
    private static final double width = 394;
    private GameState state = GameState.GAME_OFF;
    private final PlayMapInfo playMapInfo = new PlayMapInfo(height);
    private final List<ClientHandler> handlerList = new ArrayList<>();
    public static List<Player> leaderboard = Database.getLeaderboardFromDb();
    private Thread thread;
    private static final int PORT = 8888;
    public static int PLAYERS_COUNT = 0;

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start(PORT);
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(this, clientSocket);
                PLAYERS_COUNT++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removePlayer(ClientHandler handler) {
        handlerList.remove(handler);
        Player playerToRemove = handler.getPlayer();
        if (playerToRemove != null) {
            playMapInfo.playersList.remove(playerToRemove);
            sendRemoveAction(playerToRemove);
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            } else {
                startGame();
            }
        }
    }

    private void sendNewPlayerAction(Player p) throws IOException {
        String jsonPlayer = gson.toJson(p);
        Action action = new Action(Action.Type.NEW_PLAYER, jsonPlayer);
        String json = gson.toJson(action);
        for (ClientHandler h : handlerList) {
            h.sendMessage(json);
        }
    }

    private void sendRemoveAction(Player p) {
        Action action = new Action(Action.Type.REMOVE_PLAYER, p.username);
        String json = gson.toJson(action);
        for (ClientHandler h : handlerList) {
            h.sendMessage(json);
        }
    }

    public void sendWantToStartAction(ClientHandler handler) {
        Action wantToStart = new Action(Action.Type.WANT_TO_START, handler.getPlayer().username);
        String json = gson.toJson(wantToStart);
        for (ClientHandler h : handlerList) {
            h.sendMessage(json);
        }
    }

    private void sendStateAction() {
        String jsonState = gson.toJson(state);
        Action action = new Action(Action.Type.STATE, jsonState);
        String json = gson.toJson(action);
        for (ClientHandler h : handlerList) {
            h.sendMessage(json);
        }
    }

    private void sendStopAction() {
        Action action = new Action(Action.Type.STOP, null);
        String json = gson.toJson(action);
        for (ClientHandler h : handlerList) {
            h.sendMessage(json);
        }
    }

    private void sendWinnerAction() {
        Player winner = findWinner();
        String jsonWinner = gson.toJson(winner);
        Action action = new Action(Action.Type.WINNER, jsonWinner);
        String json = gson.toJson(action);
        for (ClientHandler p : handlerList) {
            p.sendMessage(json);
        }
    }

    private void sendGameInfoAction(Action.Type type) {
        String jsonInfo = gson.toJson(playMapInfo);
        Action action = new Action(type, jsonInfo);
        String json = gson.toJson(action);
        for (ClientHandler h : handlerList) {
            h.sendMessage(json);
        }
    }

    public void sendWantToPauseAction(ClientHandler handler) {
        Action action = new Action(Action.Type.WANT_TO_PAUSE, handler.getPlayer().username);
        String json = gson.toJson(action);
        for (ClientHandler h : handlerList) {
            h.sendMessage(json);
        }
    }

    public boolean isNicknameExists(String nickname) {
        for (Player p : playMapInfo.playersList) {
            if (p.username.equals(nickname))
                return true;
        }
        return false;
    }

    private boolean isColorExists(String color) {
        for (Player p : playMapInfo.playersList) {
            if (p.color.equals(color)) return true;
        }
        return false;
    }

    public boolean correctPlayersAmount() {
        return PLAYERS_COUNT <= 4;
    }

    public void addNewPlayer(String nickname, ClientHandler handler) throws IOException {
        String color = colors[random.nextInt(colors.length)];
        while (isColorExists(color)) color = colors[random.nextInt(colors.length)];

        Player newPlayer = new Player(nickname, color);
        playMapInfo.playersList.add(newPlayer);
        handler.setPlayer(newPlayer);

        sendNewPlayerAction(newPlayer);
        handlerList.add(handler);

        String jsonInfo = gson.toJson(playMapInfo);
        handler.sendMessage(jsonInfo);
    }

    private boolean allWantToStart() {
        for (Player p : playMapInfo.playersList)
            if (!p.wantToStart) return false;
        return true;
    }

    public void startGame() {
        if (allWantToStart() && !playMapInfo.playersList.isEmpty()) {
            setArrowStartY();
            state = GameState.GAME_ON;
            sendStateAction();
            thread = new Thread(() -> {
                try {
                    while (!isGameOver()) {
                        if (state == GameState.GAME_PAUSED) pause();
                        gameLogicUpdate();
                        sendGameInfoAction(Action.Type.UPDATE);
                        Thread.sleep(10);
                    }
                    sendWinnerAction();
                } catch (InterruptedException e) {
                    sendStopAction();
                } finally {
                    resetInfo();
                    sendGameInfoAction(Action.Type.RESET);
                    state = GameState.GAME_OFF;
                    sendStateAction();
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    private Player findWinner() {
        Player winner;
        winner = playMapInfo.playersList.get(0);
        for (Player p : playMapInfo.playersList) {
            if (p.score > winner.score) {
                winner = p;
            }
        }
        Database.updateDataDb(winner);
        return winner;
    }

    private void resetInfo() {
        for (Player p : playMapInfo.playersList) {
            p.score = 0;
            p.shots = 0;
            p.isShooting = false;
            p.wantToPause = false;
            p.wantToStart = false;
            p.arrow.x = 4.0;
        }
        playMapInfo.bigTarget.y = 0.5 * height;
        playMapInfo.bigTarget.direction = 1;
        playMapInfo.smallTarget.y = 0.5 * height;
        playMapInfo.smallTarget.direction = 1;
    }

    private boolean isGameOver() {
        for (Player p : playMapInfo.playersList) {
            if (p.score > 5) return true;
        }
        return false;
    }

    public boolean isGameStarted() {
        return state == GameState.GAME_ON || state == GameState.GAME_PAUSED;
    }

    private void setArrowStartY() {
        final int div = playMapInfo.playersList.size() / 2;
        final int mod = playMapInfo.playersList.size() % 2;
        for (int i = 0; i < playMapInfo.playersList.size(); ++i) {
            playMapInfo.playersList.get(i).arrow.y = 0.5 * height + 50.0 * (i - div) + (1 - mod) * 25.0;
        }
    }

    private void gameLogicUpdate() {
        nextTargetPosition(playMapInfo.bigTarget);
        nextTargetPosition(playMapInfo.smallTarget);
        for (Player p : playMapInfo.playersList) {
            if (p.isShooting) {
                p.arrow.x += p.arrow.moveSpeed;
                if (hitTarget(p.arrow, playMapInfo.bigTarget)) {
                    p.score += 1;
                    p.isShooting = false;
                    p.arrow.x = 5.0;
                } else if (hitTarget(p.arrow, playMapInfo.smallTarget)) {
                    p.score += 2;
                    p.isShooting = false;
                    p.arrow.x = 5.0;
                } else if (p.arrow.x + 45.0 > width) {
                    p.isShooting = false;
                    p.arrow.x = 5.0;
                }
            }
        }
    }

    private void nextTargetPosition(TargetModel c) {
        if (c.y + c.radius + c.speed > height || c.y - c.radius - c.speed < 0.0)
            c.direction *= -1;
        c.y += c.direction * c.speed;
    }

    boolean hitTarget(ArrowInfo a, TargetModel c) {
        return sqrt((a.x + 45.0 - c.x) * (a.x + 45.0 - c.x) + (a.y - c.y) * (a.y - c.y)) < c.radius;
    }

    synchronized void resume() {
        this.notifyAll();
    }

    synchronized void pause() throws InterruptedException {
        this.wait();
    }

    private boolean allWantToPause() {
        for (Player p : playMapInfo.playersList) {
            if (!p.wantToPause) return false;
        }
        return true;
    }

    private boolean allWantToResume() {
        for (Player p : playMapInfo.playersList) {
            if (p.wantToPause) return false;
        }
        return true;
    }

    public void pauseGame() {
        switch (state) {
            case GAME_PAUSED:
                if (allWantToResume()) {
                    state = GameState.GAME_ON;
                    sendStateAction();
                    resume();
                }
                break;
            case GAME_ON:
                if (allWantToPause()) {
                    state = GameState.GAME_PAUSED;
                    sendStateAction();
                }
                break;
        }
    }
}
