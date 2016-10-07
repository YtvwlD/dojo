package tddtrainer.gui.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import tddtrainer.catalog.CatalogDatasourceIF;
import tddtrainer.catalog.Exercise;
import tddtrainer.events.ExerciseEvent;

public class ExerciseSelectorController extends BorderPane {

    @FXML
    private ListView<Exercise> exerciseList;
    @FXML
    private TextArea descriptionField;
    @FXML
    private Button selectButton;
    @FXML
    private Button cancelButton;
    @FXML
    private HBox sliderPane;
    @FXML
    private CheckBox activateBabyStepsCheckBox;
    @FXML
    private Label codeTimeLabel;
    @FXML
    private Slider codeTimeSlider;

    private Exercise selectedExercise;
    private EventBus bus;

    private String location = "https://gist.githubusercontent.com/bendisposto/22c56ad002e562b14beea0449b981b0d/raw/f968a2dbebc4830ed94e4e47beb25e50c9901288/catalog.xml";

    Logger logger = LoggerFactory.getLogger(ExerciseSelectorController.class);
    private ExerciseSelector selector;

    @Inject
    public ExerciseSelectorController(FXMLLoader loader, EventBus bus, ExerciseSelector selector) {
        this.bus = bus;
        this.selector = selector;
        this.bus.register(this);
        URL resource = getClass().getResource("ExerciseSelector.fxml");
        loader.setLocation(resource);
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException e) {
            logger.error("Error loading Exercise selector view", e);
        }
    }

    private InputStream getDatasourceStream() {
        InputStream xmlStream = null;
        try {
            logger.debug("Fetch Catalog from {}", location);
            URL url = new URL(location);
            xmlStream = url.openStream();
        } catch (IOException e) {
            showFailedToLoadCatalogAlert(e.getClass().getSimpleName(), e.getLocalizedMessage());
            System.exit(-1);
        }
        return xmlStream;
    }

    private void showFailedToLoadCatalogAlert(String exceptionName, String excptionMessage) {
        logger.error("Error fetching catalog. {} : {}", exceptionName, excptionMessage);
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error fetching catalog");
        alert.setHeaderText(null);
        alert.setContentText(String.format(
                "The catalog could not be downloaded. \n\n"
                        + "The cause is %s : %s \n\n"
                        + "Please verify that you are online. "
                        + "If the problem still occurs please notify the administrator. \n\n"
                        + "The program will now be terminated.",
                exceptionName, excptionMessage));
        alert.showAndWait();
    }

    @FXML
    public void initialize() {
        CatalogDatasourceIF dataSource = selector.getDataSource();
        dataSource.setXmlStream(getDatasourceStream());
        exerciseList.setItems(FXCollections.observableArrayList(dataSource.loadCatalog()));

        exerciseList.setCellFactory(param -> {

            ListCell<Exercise> cell = new ListCell<Exercise>() {
                @Override
                protected void updateItem(Exercise item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.getName());
                    }
                }
            };

            return cell;
        });

        exerciseList.getSelectionModel().selectedItemProperty().addListener((o, oldValue, newValue) -> {
            selectButton.setDisable(false);
            descriptionField.setText(newValue.getDescription());
        });

        EventHandler<KeyEvent> eventHandler = (e) -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.ESCAPE) {
                exerciseList.getParent().fireEvent(e);
            }
        };

        exerciseList.addEventFilter(KeyEvent.KEY_PRESSED, eventHandler);
        descriptionField.addEventFilter(KeyEvent.KEY_PRESSED, eventHandler);

        codeTimeLabel.textProperty().bind(Bindings.format("%.0fs", codeTimeSlider.valueProperty()));
        // testTimeLabel.textProperty().bind(Bindings.format("%.0fs",
        // testTimeSlider.valueProperty()));

        sliderPane.setVisible(false);
        Platform.runLater(() -> exerciseList.requestFocus());
    }

    public void selectButtonAction() {
        Exercise selectedExercise = exerciseList.getSelectionModel().getSelectedItem();
        if (selectedExercise != null) {
            selectedExercise.setBabyStepsActivated(activateBabyStepsCheckBox.isSelected());
            selectedExercise.setBabyStepsCodeTime((int) codeTimeSlider.getValue());
            this.selectedExercise = selectedExercise;
        }
        bus.post(new ExerciseEvent(selectedExercise));
    }

    public void cancelButtonAction() {
        selectedExercise = null;
    }

    public void checkBabySteps() {
        sliderPane.setVisible(activateBabyStepsCheckBox.isSelected());
    }

    public Exercise getSelectedExercise() {
        return selectedExercise;
    }

}
