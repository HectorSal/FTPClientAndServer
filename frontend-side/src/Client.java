import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import util.Operation;
import util.UtilityFunctions;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import static util.UtilityFunctions.showConfirmation;
import static util.UtilityFunctions.showError;

public class Client extends Application {
    private TextField username, password;
    private Button signIn, back, connect;
    private Socket socket;
    private ClientHandler clientHandler;
    private TextFlow textFlow;
    private final Background focusedBackground = new Background(new BackgroundFill(Color.LIGHTBLUE, CornerRadii.EMPTY, Insets.EMPTY));
    private final Background unfocusedBackground = new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY));
    private List<File> selectedFiles = null;
    private String[] filesFoldersRoot;

    public void start(Stage stage) {
        stage.setTitle("FTP Application");
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(20, 20, 20, 20));
        gridPane.setAlignment(Pos.CENTER);

        VBox vBox = getVBox();
        gridPane.add(vBox, 0, 0);

        Scene scene = new Scene(gridPane, 400, 400);
        stage.setScene(scene);
        stage.show();

        try {
            socket = new Socket("127.0.0.1", 2000);
        } catch (IOException e) {
            showError("Exception Occurred in ClientJeopardy start method: " + e.toString());
        }
        clientHandler = new ClientHandler(socket);
        signIn.setOnAction(e -> {
            boolean isVerified = clientHandler.verifyMe(this.username.getText(), this.password.getText());
            if (isVerified) {
                showConfirmation("User is verified");
                stage.setScene(getMainScene(stage));
                stage.setResizable(false);
                stage.show();
                showFilesInRoot(true);
            } else {
                showError("User Data not found");
            }
        });
      }

    private void showFilesInRoot(boolean isForRoot) {
        if (isForRoot) {
            filesFoldersRoot = this.clientHandler.getAllFilesFolders();
            System.out.println("DONE HERE SHOW FILE");
        }
        if (filesFoldersRoot.length > 1) {
            for (String string : filesFoldersRoot) {
                HBox hBox = new HBox();
                hBox.setSpacing(8);
                hBox.setAlignment(Pos.CENTER);
                hBox.getChildren().addAll(new Text(string));
                hBox.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        hBox.requestFocus();
                        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                            if (mouseEvent.getClickCount() == 2) {
                                String text = ((Text) hBox.getChildren().get(1)).getText();
                                String[] strings = text.split("\\.");
                                if (strings.length > 1) {
                                    showConfirmation(text + " file will be downloaded soon!");
                                    new Thread(() -> {
                                        boolean isDownloaded = clientHandler.downloadSelectedFile(text);
                                        if (isDownloaded) {
                                            showConfirmation("File downloaded: " + text);
                                        } else {
                                            showError("File is not downloaded!: " + text);
                                        }
                                    }).start();
                                }
                            }
                        }
                    }
                });
                hBox.backgroundProperty().bind(Bindings
                        .when(hBox.focusedProperty())
                        .then(focusedBackground)
                        .otherwise(unfocusedBackground)
                );
                textFlow.getChildren().addAll(hBox, new Text("\n"));
            }
        }
    }

    private VBox getVBox() {
        username = getTextField("Enter Username");
        password = getTextField("Enter Password");

        signIn = getButton("Sign In", 65);

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.getChildren().add(signIn);

        VBox vBox = new VBox();
        vBox.setSpacing(10);
        vBox.getChildren().addAll(username, password, hBox);
        return vBox;
    }

    private TextField getTextField(String placeholderText) {
        if (placeholderText.contains("name")) {
            TextField textField = new TextField();
            textField.setPromptText(placeholderText);
            textField.setMinWidth(260);
            textField.setPadding(new Insets(10, 10, 10, 10));
            return textField;
        } else {
            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText(placeholderText);
            passwordField.setMinWidth(260);
            passwordField.setPadding(new Insets(10, 10, 10, 10));
            return passwordField;
        }
    }

    private Button getButton(String text, int width) {
        Button button = new Button(text);
        button.setMinWidth(width);
        return button;
    }

    private Scene getMainScene(Stage stage) {
        GridPane gridPane = new GridPane();

        back = getButton("Back", 65);
        back.setPadding(new Insets(5, 5, 5, 5));

        connect = getButton("Connect", 135);
        HBox hBoxTop = new HBox();
        hBoxTop.setSpacing(10);
        hBoxTop.getChildren().addAll(back, connect);
        
        connect.setOnAction(e -> {
          this.clientHandler.connect();
        });

        textFlow = new TextFlow();
        textFlow.setPadding(new Insets(10, 10, 10, 10));
        textFlow.setStyle("-fx-background-color: white");
        textFlow.setPrefWidth(1000);
        textFlow.setPrefHeight(300);

        ScrollPane scrollPane = new ScrollPane(textFlow);

        Button upload = this.getButton("Upload", 135);
        Button selectImage = this.getButton("Select File(s)", 135);
        SimpleStringProperty simpleStringProp = new SimpleStringProperty("Select File(s)");

        Label selectedFilesName = new Label();
        selectedFilesName.textProperty().bind(simpleStringProp);
        selectImage.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Image to upload");
            fileChooser.getExtensionFilters().addAll(
                    new ExtensionFilter("Text Files", "*.txt"),
                    new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"),
                    new ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac"),
                    new ExtensionFilter("All Files", "*.*")
            );
            selectedFiles = fileChooser.showOpenMultipleDialog(stage);
            boolean isPresent = false;
            if (selectedFiles != null) {
                if (selectedFiles.size() <= 5) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (File file : selectedFiles) {
                        stringBuilder.append(file.getName()).append(", ");
                        if (Arrays.asList(filesFoldersRoot).contains(file.getName())) {
                            isPresent = true;
                            showError("Selected file(s) already exists in the directory");
                            selectedFiles = null;
                            break;
                        }
                    }
                    if (isPresent) {
                        simpleStringProp.set("Select File(s)");
                    } else {
                        simpleStringProp.set(stringBuilder.toString());
                    }
                } else {
                    UtilityFunctions.showError("Select upto 5 Files");
                }
            } else {
                UtilityFunctions.showError("No File Selected");
                simpleStringProp.set("Select File(s)");
            }
        });

        upload.setOnAction(e -> {
            if (selectedFiles != null) {
                this.clientHandler.sendFiles(selectedFiles);
            } else {
                showError("Please Select Files to upload");
            }
        });

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER);
        hBox.getChildren().addAll(upload, selectImage, selectedFilesName);

        VBox vBox = new VBox();
        vBox.setSpacing(10);
        vBox.getChildren().addAll(hBoxTop, scrollPane, hBox);
        gridPane.add(vBox, 0, 0);
        gridPane.setAlignment(Pos.CENTER);
        return new Scene(gridPane, 1100, 400);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class ClientHandler {
    // private ObjectInputStream objectInputStream;
    // private ObjectOutputStream objectOutputStream;
    private PrintWriter objectOutputStream;
    private BufferedReader objectInputStream;

    private BufferedOutputStream output;

    private Socket dataSocket;

    public ClientHandler(Socket socket) {
        if (socket != null) {
            try {
              this.dataSocket = socket;
                // this.objectInputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // this.objectOutputStream new PrintWriter(socket.getOutputStream(), true);
            this.objectInputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.objectOutputStream = new PrintWriter(socket.getOutputStream(), true);
            this.output = new BufferedOutputStream(socket.getOutputStream()); 
            
            } catch (IOException ex) {
              ex.printStackTrace();
                showError("Exception Occurred in SingleClientThread Constructor: " + ex.toString());
            }
            return;
        }
        showError("Server is not running");
    }

    public boolean verifyMe(String username, String password) {
      try {
          // Read initial message from server
          String initialMessage = this.objectInputStream.readLine();
          System.out.println("Initial Message from Server: " + initialMessage);

          // Send USER command with the username
          this.objectOutputStream.println(Operation.USER + " " +username);
          this.objectOutputStream.flush();

          // Read response from the server for USER command
          String userResponse = this.objectInputStream.readLine();
          System.out.println("Response to USER command: " + userResponse);

          // Check if USER command was successful
          if (userResponse.startsWith("331")) { // Assuming '331' is the response code for successful USER command
              // Send PASS command with the password
              this.objectOutputStream.println(Operation.PASS + " " + password);
              this.objectOutputStream.flush();

              // Read response from the server for PASS command
              String passResponse = this.objectInputStream.readLine();
              System.out.println("Response to PASS command: " + passResponse);

              // Check if PASS command was successful
              return passResponse.startsWith("230"); // Assuming '230' is the response code for successful PASS command
          }
      } catch (IOException e) {
          showError("Exception Occurred in ClientHandler verifyMe: " + e.toString());
      }
      return false;
  }

    public String[] getAllFilesFolders() {
        try {
            this.objectOutputStream.println(Operation.READ_DIRECTORY);
            return ( this.objectInputStream.readLine()).split(",");
        } catch (IOException e) {
            showError("Exception Occurred in ClientHandler getAllFilesFolders: " + e.toString());
        }
        return null;
    }

    public void sendFiles(List<File> allFilesPaths) {
    try {
        for (File file : allFilesPaths) {
          System.out.println("FILE" + file);
            // Send STOR command with the file name
            this.objectOutputStream.println(Operation.STOR + " " + file.getName());
            // this.objectOutputStream.println(file.getName());
            
            // Wait for server response to proceed
            String serverResponse = this.objectInputStream.readLine();
            System.out.println("Server response: " + serverResponse);

            // Check if server is ready to receive file data
            if (serverResponse.startsWith("150")) {
                // Open a new data connection to send file data
                try (
                     BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
                     BufferedOutputStream dataOutputStream = new BufferedOutputStream(dataSocket.getOutputStream())) {

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                        dataOutputStream.write(buffer, 0, bytesRead);
                    }
                    dataOutputStream.flush();
                }

                // Read the final response from server after file transfer
                String finalResponse = this.objectInputStream.readLine();
                System.out.println("Final response from server: " + finalResponse);
            }
        }
    } catch (IOException ex) {
        showError("Exception occurred in ClientHandler sendFiles: " + ex.toString());
    }
}

          /**
     * 
     */
    public void connect() {
      try {
         this.objectOutputStream.println(Operation.PASV);

          // Read response from the server for PASV command
          String userResponse = this.objectInputStream.readLine();
          System.out.println("Response to PASV command: " + userResponse);

          int indexOfParentheses = userResponse.indexOf('(');
          String[] addressAndPort = userResponse.substring(indexOfParentheses + 1).split(",");
          String address = addressAndPort[0] + "." + addressAndPort[1] + "." + addressAndPort[2] + "." + addressAndPort[3];
          int length = addressAndPort[5].length();
          addressAndPort[5] = addressAndPort[5].substring(0, length - 1);
          int port = Integer.valueOf(addressAndPort[4]) * 256 + Integer.valueOf(addressAndPort[5]);

          this.dataSocket = new Socket(address, port);

          this.objectInputStream = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
          this.objectOutputStream = new PrintWriter(dataSocket.getOutputStream(), true);
          this.output = new BufferedOutputStream(dataSocket.getOutputStream());
      }
      catch (Exception e) {
          e.printStackTrace();
      }

    }

    public boolean downloadSelectedFile(String fileName) {
        try {
            this.objectOutputStream.println(Operation.DOWNLOAD_FILE);
            this.objectOutputStream.println(fileName);

            String currentCompletePath = System.getProperty("user.home") + "\\Downloads\\" + fileName;
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(new File(currentCompletePath)));
            // byte[] arrayReceived = (byte[]) this.objectInputStream.readLine();
            // bufferedOutputStream.write(arrayReceived, 0, arrayReceived.length);
            bufferedOutputStream.flush();
            return true;
        } catch (IOException  | ClassCastException e) {
            showError("Exception occurred in SingleClientHandler: " + e.toString());
        }
        return false;
    }

    private void closeDataConnection() {
      try {
          if (dataSocket != null) {
              dataSocket.close();
          }
      }
      catch (IOException e) {
          e.printStackTrace();
      }
      dataSocket = null;
    }
}