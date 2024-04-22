package atomniyivan.archery_game.client;

import atomniyivan.archery_game.hibernate.Database;
import atomniyivan.archery_game.models.ArrowModel;
import atomniyivan.archery_game.models.PlayMapInfo;
import atomniyivan.archery_game.models.Player;
import atomniyivan.archery_game.server.GameServer;
import atomniyivan.archery_game.server.ServerHandler;
import atomniyivan.archery_game.states.Action;
import atomniyivan.archery_game.states.GameState;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class GameClient {
    @FXML
    AnchorPane gameBar;
    @FXML
    Circle bigTarget;
    @FXML
    Circle smallTarget;
    @FXML
    VBox playersMenu;
    @FXML
    VBox playersInfoMenu;
    public static final Gson gson = new Gson();
    ServerHandler serverHandler;
    GameState state = GameState.GAME_OFF;

    public void connectServer(Socket socket, DataInputStream dis, DataOutputStream dos) {
        serverHandler = new ServerHandler(this, socket, dis, dos);
    }

    @FXML
    void startGame() {
        if (state == GameState.GAME_OFF) {
            String jsonStart = gson.toJson(Action.Type.WANT_TO_START);
            serverHandler.sendMessage(jsonStart);
        }
    }

    @FXML
    void stopGame() {
        if (state != GameState.GAME_OFF) {
            String json = gson.toJson(Action.Type.WANT_TO_PAUSE);
            serverHandler.sendMessage(json);
        }
    }

    @FXML
    void Shoot() {
        if (state == GameState.GAME_ON) {
            String json = gson.toJson(Action.Type.SHOOT);
            serverHandler.sendMessage(json);
        }
    }
    @FXML
    void showWinners(){
        if (state != GameState.GAME_ON){
            GameServer.leaderboard = Database.getLeaderboardFromDb();
        }
        TextArea leaderboardTextArea = new TextArea();
        leaderboardTextArea.setEditable(false);
        leaderboardTextArea.setWrapText(true);
        leaderboardTextArea.appendText("name\twins\n");
        GameServer.leaderboard.forEach(player -> {
            leaderboardTextArea.appendText(player.username + "\t\t\t" + player.wins + "\n");
        });

        Stage leaderboardStage = new Stage();
        leaderboardStage.setTitle("Leaderboard");

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.getChildren().add(leaderboardTextArea);

        Scene scene = new Scene(root, 300, 200);
        leaderboardStage.setScene(scene);
        leaderboardStage.show();
    }

    public void addPlayersToGame(final PlayMapInfo playMapInfo) {
        for (Player p : playMapInfo.playersList) {
            addPlayer(p);
        }
    }

    public void addPlayer(final Player p) {
        Platform.runLater(() -> {
            Polygon playerMarker = new Polygon(-57.0, -33.0, -57.0, 22.0, -16.0, -5.0);
            playerMarker.setId(p.username + "Triangle");
            playerMarker.setFill(Color.valueOf(p.color));
            if (p.wantToStart) playerMarker.setStroke(Color.GREEN);
            VBox pane1 = new VBox(playerMarker);
            pane1.setId(p.username + "Icon");
            pane1.setAlignment(Pos.CENTER);
            pane1.setPadding(new Insets(10));
            playersMenu.getChildren().add(pane1);

            Label score = new Label(p.username + " счёт:");
            score.setTextFill(Color.valueOf("#2c3337"));
            score.setId(p.username + "Score");

            Label scoreCount = new Label(String.valueOf(p.score));
            scoreCount.setTextFill(Color.valueOf("#2c3337"));
            scoreCount.setId(p.username + "ScoreCount");

            Label shots = new Label(p.username + " выстрелы:");
            shots.setTextFill(Color.valueOf("#2c3337"));
            shots.setId(p.username + "Shots");

            Label shotsCount = new Label(String.valueOf(p.shots));
            shotsCount.setTextFill(Color.valueOf("#2c3337"));
            shotsCount.setId(p.username + "ShotsCount");

            VBox pane = new VBox( score, scoreCount, shots, shotsCount);
            pane.setAlignment(Pos.CENTER);
            pane.setId(p.username + "VBox");
            playersInfoMenu.getChildren().add(pane);
        });
    }

    private Polygon getPlayerMarker(final String nickname) {
        return (Polygon) gameBar.getScene().lookup("#" + nickname + "Triangle");
    }

    private ArrowModel getArrow(final String nickname) {
        return (ArrowModel) gameBar.getScene().lookup("#" + nickname + "Arrow");
    }

    private Label getShotsCountLabel(final String nickname) {
        return (Label) gameBar.getScene().lookup("#" + nickname + "ShotsCount");
    }

    private Label getScoreCountLabel(final String nickname) {
        return (Label) gameBar.getScene().lookup("#" + nickname + "ScoreCount");
    }

    private VBox getVBox(final String nickname) {
        return (VBox) gameBar.getScene().lookup("#" + nickname + "VBox");
    }

    public void setPlayerWantToStart(final String nickname) {
        Polygon playerMarker = getPlayerMarker(nickname);
        playerMarker.setStroke(Color.GREEN);
    }

    public void updateGameInfo(final PlayMapInfo playMapInfo) {
        Platform.runLater(() -> {
            smallTarget.setLayoutY(playMapInfo.smallTarget.y);
            bigTarget.setLayoutY(playMapInfo.bigTarget.y);

            for (Player p : playMapInfo.playersList) {
                ArrowModel playerArrow = getArrow(p.username);
                if (p.isShooting) {
                    if (playerArrow == null) {
                        playerArrow = createArrow(p);
                        setShots(p);
                    }
                    playerArrow.setLayoutX(p.arrow.x);
                } else if (playerArrow != null) {
                    removeArrow(playerArrow);
                    setScore(p);
                }
            }
        });
    }

    private ArrowModel createArrow(final Player p) {
        ArrowModel arrow = new ArrowModel(0, 0, 45, 0, 7.0, p.color);
        arrow.setLayoutX(4);
        arrow.setLayoutY(p.arrow.y);
        arrow.setId(p.username + "Arrow");
        gameBar.getChildren().add(arrow);
        return arrow;
    }

    private void removeArrow(final ArrowModel arrow) {
        gameBar.getChildren().remove(arrow);
    }


    private void setScore(final Player p) {
        final Label scoreLabel = getScoreCountLabel(p.username);
        scoreLabel.setText(String.valueOf(p.score));
    }

    public void setShots(final Player player) {
        Label shotsLabel = getShotsCountLabel(player.username);
        shotsLabel.setText(String.valueOf(player.shots));
    }

    public void updatePlayerWantToPause(final String playerColor) {
        Polygon playerMarker = getPlayerMarker(playerColor);
        if (playerMarker.getStroke() == Color.GREEN)
            playerMarker.setStroke(Color.RED);
        else playerMarker.setStroke(Color.GREEN);
    }

    public void setState(final GameState state) {
        this.state = state;
    }

    public void showWinner(final Player p) {
        Platform.runLater(() -> {
            String info = p.username + " одержал победу с " + p.score + " очками!";
            Alert alert = new Alert(Alert.AlertType.INFORMATION, info);
            alert.show();
        });
    }

    public void resetGameInfo(final PlayMapInfo playMapInfo) {
        Platform.runLater(() -> {
            smallTarget.setLayoutY(playMapInfo.smallTarget.y);
            bigTarget.setLayoutY(playMapInfo.bigTarget.y);

            for (Player p : playMapInfo.playersList) {
                setShots(p);
                setScore(p);
                gameBar.getChildren().remove(getArrow(p.username));
                getPlayerMarker(p.username).setStroke(Color.TRANSPARENT);
            }
        });
    }

    public void removePlayer(final String nickname) {
        Platform.runLater(() -> {
            gameBar.getChildren().remove(getArrow(nickname));
            playersMenu.getChildren().remove(getPlayerMarker(nickname));
            playersInfoMenu.getChildren().remove(getVBox(nickname));
        });
    }

    public void showStop() {
        Platform.runLater(() -> {
            String info = "Игра была остановлена.";
            Alert alert = new Alert(Alert.AlertType.WARNING, info);
            alert.show();
        });
    }
}

