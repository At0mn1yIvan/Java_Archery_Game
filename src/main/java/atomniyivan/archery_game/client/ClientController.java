package atomniyivan.archery_game.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientController {
    @FXML
    private TextField nameTextField;
    private Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private static final int PORT = 8888;

    @FXML
    private void initialize() {
        try {
            clientSocket = new Socket("localhost", PORT);
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void onJoinButtonClick() {
        try {
            out.writeUTF(nameTextField.getText());
            String response = in.readUTF();
            if (response.equals("OK")) {
                FXMLLoader fxmlLoader = new FXMLLoader(GameClientLaunch.class.getResource("game-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load());
                Stage stage = (Stage) nameTextField.getScene().getWindow();
                stage.setScene(scene);
                GameClient client = fxmlLoader.getController();
                client.connectServer(clientSocket, in, out);
            }
            else if (response.equals("MAX_PLAYERS_REACHED")) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Достигнуто максимальное число игроков.");
                alert.show();
            }
            else {
                Alert alert = new Alert(Alert.AlertType.WARNING, response);
                alert.showAndWait();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}