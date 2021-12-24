package controllers;

import bodyComponentsPaths.BodyComponentsPaths;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import resources.checker.ResourceChecker;
import target.Graph;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

public class PrimaryController {
    //--------------------------------------------------Members-----------------------------------------------------//
    private Stage primaryStage;
    private Graph graph = null;

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private ScrollPane statusBar;

    @FXML
    private Button graphDetailsButton;

    @FXML
    private Button connectionsButton;

    @FXML
    private Button taskButton;

    @FXML
    private Menu file;

    @FXML
    private MenuItem loadXMLButton;

    @FXML
    private MenuItem saveProgressButton;

    @FXML
    private MenuItem exitButton;

    @FXML
    private Menu animations;

    @FXML
    private CheckBox enableAnimations;

    @FXML
    private Menu themes;

    @FXML
    private RadioMenuItem defaultTheme;

    @FXML
    private RadioMenuItem darkModeTheme;

    @FXML
    private RadioMenuItem rainbowTheme;

    @FXML
    private Menu Help;

    @FXML
    private MenuItem about;

    @FXML
    private AnchorPane StatusBar;
    private SimpleStringProperty selectedFileProperty;
    private SimpleBooleanProperty isFileSelected;

    //--------------------------------------------------Toolbar-----------------------------------------------------//
    @FXML
    void aboutPressed(ActionEvent event)
    {

    }

    @FXML
    void enableAnimationsPressed(ActionEvent event) {

    }

    @FXML
    void loadXMLButtonPressed(ActionEvent event)
    {
        ResourceChecker rc = new ResourceChecker();
        FileChooser fileChooser = new FileChooser();
        File selectedFile;
        fileChooser.setTitle("Select a file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("xml files", "*.xml"));

        selectedFile = fileChooser.showOpenDialog(primaryStage);
        if(selectedFile == null)
            return;

        if(!OverrideGraph())
            return;

        try{
            graph = rc.extractFromXMLToGraph(selectedFile.toPath());
            FileLoadedSuccessfully();
        }
        catch(Exception ex)
        {
            ErrorPopup(ex, "Error loading file");
        }
    }

    @FXML
    void saveProgressPressed(ActionEvent event) {

    }
    //--------------------------------------------------Themes-----------------------------------------------------//
    @FXML
    void defaultThemePressed(ActionEvent event) {
        Scene scene = primaryStage.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("src/userInterface/Stylesheets/LightMode.css")).toExternalForm());
    }

    @FXML
    void darkModeThemePressed(ActionEvent event) {
        Scene scene = primaryStage.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("src/userInterface/Stylesheets/DarkMode.css")).toExternalForm());
    }

    @FXML
    void rainbowThemePressed(ActionEvent event) {

    }
    //--------------------------------------------------Sidebar-----------------------------------------------------//
    @FXML
    void connectionsButtonPressed(ActionEvent event) {

    }

    @FXML
    void graphDetailsButtonPressed(ActionEvent event) throws Exception
    {
        Platform.runLater(()->{
            FXMLLoader fxmlLoader = new FXMLLoader();
            String componentName = BodyComponentsPaths.SHOW_DETAILS;
            URL url = getClass().getResource(componentName);
            fxmlLoader.setLocation(url);
            ScrollPane newPane = null;
            try {
                newPane = (ScrollPane) fxmlLoader.load(url.openStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            mainBorderPane.setCenter(newPane);

        });
    }


    @FXML
    void taskButtonPressed(ActionEvent event) {

    }

    //--------------------------------------------------Methods-----------------------------------------------------//
    public void setPrimaryStage(Stage stage){
        this.primaryStage = stage;
    }

    private Boolean OverrideGraph()
    {
        if(graph == null)
            return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Override existed graph");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to override the graph " + graph.getGraphName() + "?");
        ButtonType yesButton = new ButtonType("Yes");
        ButtonType noButton = new ButtonType("No");
        alert.getButtonTypes().setAll(yesButton, noButton );
        Optional<ButtonType> result = alert.showAndWait();

        return result.get() == yesButton;
    }

    private void FileLoadedSuccessfully()
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("File loaded Successfully");
        alert.setHeaderText(null);
        alert.setContentText("The graph " + graph.getGraphName() + " loaded successfully!");
        alert.showAndWait();
    }

    private void ErrorPopup(Exception ex, String title)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }
}