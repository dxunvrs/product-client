package gui;

import gui.locale.I18n;
import gui.locale.LocaleManager;
import gui.locale.Strings;
import gui.service.ClientService;
import gui.view.LoginView;
import gui.view.MainView;
import gui.view.VisualizationPane;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import network.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GUIClient extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GUIClient.class);

    public static final String defaultHost = "localhost";
    public static final int defaultPort = 1984;

    private Stage primaryStage;

    private ConnectionManager connectionManager;
    private ClientService service;

    // Locale DI
    private final Strings strings = new Strings();
    private final LocaleManager localeManager = new LocaleManager(strings);
    private final I18n i18n = new I18n(localeManager);


    @Override
    public void start(Stage stage) {
        try {
            connectionManager = new ConnectionManager(defaultHost, defaultPort);
        } catch (Exception e) {
            logger.error("Socket open exception", e);
        }
        service = new ClientService(connectionManager);

        this.primaryStage = stage;

        primaryStage.setTitle(localeManager.t("app.title"));
        localeManager.localeProperty().addListener((o, a, b) ->
                primaryStage.setTitle(localeManager.t("app.title")));

        showLogin();
        primaryStage.show();
    }

    public void showLogin() {
        LoginView view = new LoginView(this::onLoggedIn, service, i18n, localeManager, strings);
        Scene scene = new Scene(view.getRoot(), 480, 360);
        primaryStage.setScene(scene);
    }

    @Override
    public void stop() {
        if (service != null) {
            try {
                service.close();
            } catch (Exception e) {
                logger.error("Socket close exception", e);
            }
        }
        Platform.exit();
    }

    public void onLoggedIn() {
        MainView view = new MainView(this::onLogout, service, new VisualizationPane(), i18n, localeManager, strings);
        Scene scene = new Scene(view.getRoot(), 1200, 760);
        primaryStage.setScene(scene);
    }

    public void onLogout() {
        showLogin();
    }
}
