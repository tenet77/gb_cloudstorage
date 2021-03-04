package client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextArea;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ClientFormController implements Initializable {

    public TextArea messageArea;
    public ListView<String> listOfFiles;
    public ListView<String> listOfFilesOnServer;
    private ConnectionService connectionService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        connectionService   = new ConnectionService(this);
        updateClientFiles();
    }

    public void updateClientFiles() {
        try {
            File dir = new File("client");
            File[] files = dir.listFiles();
            if (files == null) return;
            ObservableList<String> listItems = FXCollections.observableArrayList();
            for (File file : files) {
                listItems.add(file.getName());
            }
            listOfFiles.setItems(listItems);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect() {

        showMessage(connectionService.openConnection());
        showMessage(connectionService.listOfFiles());

    }

    public void updateFiles() {
        showMessage(connectionService.listOfFiles());
    }

    public void upload() {
        MultipleSelectionModel<String> msm = listOfFiles.getSelectionModel();
        String item = msm.getSelectedItem();
        if (item != null) {
            showMessage(connectionService.upload(item));
        }
        else showMessage("select file");
    }

    public void download() {
        MultipleSelectionModel<String> msm = listOfFilesOnServer.getSelectionModel();
        String item = msm.getSelectedItem();
        if (item != null) {
            showMessage(connectionService.download(item));
        }
        else showMessage("select file");
    }

    public void remove() {
        MultipleSelectionModel<String> msm = listOfFilesOnServer.getSelectionModel();
        String item = msm.getSelectedItem();
        if (item != null) {
            showMessage(connectionService.remove(item));
        }
        else showMessage("select file");
    }

    public void showMessage(String message) {
        messageArea.appendText(message);
        messageArea.appendText("\n");
    }

    public void updateListOfFilesOnServer(List<String> list) {
        ObservableList<String> listItems = FXCollections.observableArrayList();
        listItems.addAll(list);
        listOfFilesOnServer.setItems(listItems);
    }
}
