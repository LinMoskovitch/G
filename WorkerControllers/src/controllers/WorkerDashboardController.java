package controllers;

import com.google.gson.Gson;
import dtos.DashboardTaskDetailsDTO;
import http.HttpClientUtil;
import tableItems.SelectedGraphTableItem;
import tableItems.WorkerTaskStatusTableItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import patterns.Patterns;
import users.UsersLists;

import java.io.IOException;
import java.util.Set;

import static controllers.WorkerPrimaryController.ShowPopUp;

public class WorkerDashboardController {
    //----------------------------------------------- My Members -------------------------------------------//
    private final ObservableList<String> onlineAdminsList = FXCollections.observableArrayList();
    private final ObservableList<String> onlineWorkersList = FXCollections.observableArrayList();
    private final ObservableList<String> onlineTasksList = FXCollections.observableArrayList();
    private ObservableList<SelectedGraphTableItem> selectedTaskTargetsList;
    private ObservableList<WorkerTaskStatusTableItem> selectedTaskStatusList;
    private WorkerPrimaryController workerPrimaryController;
    private String username;
    private PullerThread pullerThread;

    //---------------------------------------------- FXML Members -------------------------------------------//
    @FXML private SplitPane SplitPane;
    @FXML private TitledPane OnlineTasksTiltedPane;
    @FXML private ListView<String> TasksListView;
    @FXML private Button RegisterToTaskButton;
    @FXML private TitledPane AdminsListView;
    @FXML private ListView<String> onlineAdminsListView;
    @FXML private TitledPane OnlineWorkersTiltedPane;
    @FXML private ListView<String> onlineWorkersListView;
    @FXML private Font x11;
    @FXML private Color x21;
    @FXML private TextField UserNameTextField;
    @FXML private TextField TotalCreditsTextField;
    @FXML private Font x1;
    @FXML private Color x2;
    @FXML private TextField TaskNameTextField;
    @FXML private TextField CreatedByTextField;
    @FXML private TableView<SelectedGraphTableItem> TaskTargetsTableView;
    @FXML private TableColumn<SelectedGraphTableItem, Integer> Targets;
    @FXML private TableColumn<SelectedGraphTableItem, Integer> Independents;
    @FXML private TableColumn<SelectedGraphTableItem, Integer> Leaves;
    @FXML private TableColumn<SelectedGraphTableItem, Integer> Middles;
    @FXML private TableColumn<SelectedGraphTableItem, Integer> Roots;
    @FXML private TableView<WorkerTaskStatusTableItem> TaskStatusTableView;
    @FXML private TableColumn<WorkerTaskStatusTableItem, String> Type;
    @FXML private TableColumn<WorkerTaskStatusTableItem, String> Status;
    @FXML private TableColumn<WorkerTaskStatusTableItem, Integer> Workers;
    @FXML private TableColumn<WorkerTaskStatusTableItem, Integer> Payment;
    @FXML private TableColumn<WorkerTaskStatusTableItem, String> Registered;

    //---------------------------------------------- Initialize -------------------------------------------//
    public void initialize(WorkerPrimaryController workerPrimaryController, String userName)
    {
        initializeTaskTargetDetailsTable();
        initializeTaskDetailsTable();
        setupListeners();

        this.workerPrimaryController = workerPrimaryController;
        this.UserNameTextField.setText(userName);
    }

    public void initializeTaskTargetDetailsTable() {
        this.Targets.setCellValueFactory(new PropertyValueFactory<SelectedGraphTableItem, Integer>("targets"));
        this.Roots.setCellValueFactory(new PropertyValueFactory<SelectedGraphTableItem, Integer>("roots"));
        this.Middles.setCellValueFactory(new PropertyValueFactory<SelectedGraphTableItem, Integer>("middles"));
        this.Leaves.setCellValueFactory(new PropertyValueFactory<SelectedGraphTableItem, Integer>("leaves"));
        this.Independents.setCellValueFactory(new PropertyValueFactory<SelectedGraphTableItem, Integer>("independents"));
    }

    public void initializeTaskDetailsTable() {
        this.Type.setCellValueFactory(new PropertyValueFactory<WorkerTaskStatusTableItem, String>("type"));
        this.Status.setCellValueFactory(new PropertyValueFactory<WorkerTaskStatusTableItem, String>("status"));
        this.Workers.setCellValueFactory(new PropertyValueFactory<WorkerTaskStatusTableItem, Integer>("workers"));
        this.Payment.setCellValueFactory(new PropertyValueFactory<WorkerTaskStatusTableItem, Integer>("totalPayment"));
        this.Registered.setCellValueFactory(new PropertyValueFactory<WorkerTaskStatusTableItem, String>("registered"));
    }

    private void setupListeners() {
        this.onlineTasksList.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                for(String curr : c.getList())
                {
                    if(!WorkerDashboardController.this.TasksListView.getItems().contains(curr))
                        WorkerDashboardController.this.TasksListView.getItems().add(curr);
                }
            }
        });
    }

    //--------------------------------------------- Puller Thread ------------------------------------------//
    public class PullerThread extends Thread
    {
        @Override public void run()
        {
            while(true)
            {
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                getOnlineUsersFromServer();
                getOnlineTasksFromServer();
            }
        }
        //------------------------ Tasks List ------------------------//
        private void getOnlineTasksFromServer() {
            String finalUrl = HttpUrl
                    .parse(Patterns.LOCAL_HOST + Patterns.TASK_LIST)
                    .newBuilder()
                    .addQueryParameter("all-tasks-list", "all-tasks-list")
                    .build()
                    .toString();

            HttpClientUtil.runAsync(finalUrl, "GET", null, new Callback() {
                @Override public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Platform.runLater(() -> System.out.println("Failure on connecting to server for task-list!"));
                }

                @Override public void onResponse(@NotNull Call call, @NotNull Response response) {
                    if (response.code() >= 200 && response.code() < 300) //Success
                    {
                        Platform.runLater(() ->
                                {
                                    Gson gson = new Gson();
                                    ResponseBody responseBody = response.body();
                                    try {
                                        if (responseBody != null) {
                                            Set taskList = gson.fromJson(responseBody.string(), Set.class);
                                            refreshTasksList(taskList);
                                            responseBody.close();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                        );
                    } else //Failed
                        Platform.runLater(() -> System.out.println("couldn't pull all-task-list from server!"));
                }
            });
        }

        private void refreshTasksList(Set<String> taskList)
        {
            if(taskList == null)
                return;

            for(String curr : taskList)
            {
                if(!WorkerDashboardController.this.onlineTasksList.contains(curr))
                    WorkerDashboardController.this.onlineTasksList.add(curr);
            }
        }

        //------------------------ Users List ------------------------//
        private void getOnlineUsersFromServer() {
            String finalUrl = HttpUrl
                    .parse(Patterns.LOCAL_HOST + Patterns.USERS_LISTS)
                    .newBuilder()
                    .build()
                    .toString();

            HttpClientUtil.runAsync(finalUrl, "GET", null, new Callback() {

                @Override public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Platform.runLater(() -> System.out.println("Failure on connecting to server for users list!"));
                }

                @Override public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    Gson gson = new Gson();
                    ResponseBody responseBody = response.body();
                    UsersLists usersLists = gson.fromJson(responseBody.string(), UsersLists.class);
                    responseBody.close();

                    Platform.runLater(() -> updateUsersLists(usersLists));
                }
            });
        }
    }

    private void updateUsersLists(UsersLists usersLists) {
        this.onlineAdminsList.clear();
        this.onlineWorkersList.clear();
        this.onlineAdminsList.addAll(usersLists.getAdminsList());
        this.onlineWorkersList.addAll(usersLists.getWorkersList());
    }

    private void createPullingThread() {
        this.pullerThread = new PullerThread();

        this.onlineAdminsListView.setItems(this.onlineAdminsList);
        this.onlineWorkersListView.setItems(this.onlineWorkersList);

        this.pullerThread.setDaemon(true);
        this.pullerThread.start();
    }

    //----------------------------------------- Task Registration  --------------------------------------------//
    @FXML void RegisterToTaskButtonPressed(ActionEvent event) {
        String selectedTaskName = this.TasksListView.getSelectionModel().getSelectedItem();

        if(selectedTaskName == null)
            return;

        String finalUrl = HttpUrl
                .parse(Patterns.LOCAL_HOST + Patterns.TASKS)
                .newBuilder()
                .addQueryParameter("register", selectedTaskName)
                .build()
                .toString();

        HttpClientUtil.runAsync(finalUrl, "GET", null, new Callback() {

            @Override public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Platform.runLater(() ->
                        ShowPopUp(Alert.AlertType.ERROR, "Error", null, e.getMessage()));
            }

            @Override public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.code() >= 200 && response.code() < 300) //Success
                        Platform.runLater(()-> ShowPopUp(Alert.AlertType.INFORMATION, "Registration Successfully!", null, response.header("message")));
                else //Failed
                    Platform.runLater(()-> ShowPopUp(Alert.AlertType.ERROR, "Registration Failed!", null, response.header("message")));
            }
        });
    }

    //------------------------------------------- Task Information  ---------------------------------------------//
    @FXML void TaskSelectedFromTaskListView(MouseEvent mouseEvent) {
        String selectedTaskName = this.TasksListView.getSelectionModel().getSelectedItem();

        if(selectedTaskName == null)
            return;

        this.RegisterToTaskButton.setDisable(false);

        String finalUrl = HttpUrl
                .parse(Patterns.LOCAL_HOST + Patterns.TASKS)
                .newBuilder()
                .addQueryParameter("task-info", selectedTaskName)
                .build()
                .toString();

        HttpClientUtil.runAsync(finalUrl, "GET", null, new Callback() {
            @Override public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Platform.runLater(() -> System.out.println("Failure on connecting to server for task-info!"));
            }

            @Override public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.code() >= 200 && response.code() < 300) //Success
                {
                    Platform.runLater(() ->
                            {
                                Gson gson = new Gson();
                                ResponseBody responseBody = response.body();
                                try {
                                    if (responseBody != null) {
                                        {
                                            DashboardTaskDetailsDTO taskDetailsDTO = gson.fromJson(responseBody.string(), DashboardTaskDetailsDTO.class);
                                            refreshWorkerTaskDetailsDTO(taskDetailsDTO);
                                            responseBody.close();
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                    );
                } else //Failed
                    Platform.runLater(() -> System.out.println("couldn't pull graph-dto from server!"));
            }

            private void refreshWorkerTaskDetailsDTO(DashboardTaskDetailsDTO taskDetailsDTO) {
                WorkerDashboardController.this.TaskNameTextField.setText(taskDetailsDTO.getTaskName());
                WorkerDashboardController.this.CreatedByTextField.setText(taskDetailsDTO.getUploader());

                updateTaskTargetDetailsTable(taskDetailsDTO);
                updateTaskStatusTable(taskDetailsDTO);
            }

            private void updateTaskTargetDetailsTable(DashboardTaskDetailsDTO taskDetailsDTO) {

                SelectedGraphTableItem selectedGraphTableItem = new SelectedGraphTableItem(taskDetailsDTO.getRoots(),
                        taskDetailsDTO.getMiddles(), taskDetailsDTO.getLeaves(), taskDetailsDTO.getIndependents());

                WorkerDashboardController.this.selectedTaskTargetsList.clear();
                WorkerDashboardController.this.selectedTaskTargetsList.add(selectedGraphTableItem);

                WorkerDashboardController.this.TaskTargetsTableView.setItems(WorkerDashboardController.this.selectedTaskTargetsList);
            }

            private void updateTaskStatusTable(DashboardTaskDetailsDTO taskDetailsDTO) {

                WorkerTaskStatusTableItem taskStatusTableItem = new WorkerTaskStatusTableItem(taskDetailsDTO.getTaskType(), taskDetailsDTO.getTaskStatus(),
                        taskDetailsDTO.getRegisteredWorkers(), taskDetailsDTO.getTotalPayment(), WorkerDashboardController.this.username);

                WorkerDashboardController.this.selectedTaskStatusList.clear();
                WorkerDashboardController.this.selectedTaskStatusList.add(taskStatusTableItem);

                WorkerDashboardController.this.TaskStatusTableView.setItems(WorkerDashboardController.this.selectedTaskStatusList);
            }
        });
    }
}
