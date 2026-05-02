package gui;

import gui.locale.I18n;
import gui.locale.LocaleManager;
import gui.locale.Strings;
import gui.service.ClientService;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GUIClient extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GUIClient.class);

    private Stage primaryStage;
    private ClientService clientService;

    private String defaultHost = "localhost";
    private int defaultPort = 1984;

    // Locale DI
    private final Strings strings = new Strings();
    private final LocaleManager localeManager = new LocaleManager(strings);
    private final I18n i18n = new I18n(localeManager);


    @Override
    public void start(Stage stage) {

    }

    @Override
    public void stop() {

    }
}
