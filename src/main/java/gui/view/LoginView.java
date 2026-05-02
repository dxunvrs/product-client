package gui.view;

import gui.locale.I18n;
import gui.locale.LocaleManager;
import gui.locale.Strings;
import gui.service.ClientService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import network.Response;
import network.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;

public class LoginView {
    private static final Logger logger = LoggerFactory.getLogger(LoginView.class);

    private final Runnable onLoggedInCallback;
    private final ClientService service;
    private final LocaleManager localeManager;
    private final VBox root;

    private final TextField usernameField;
    private final PasswordField passwordField;
    private final PasswordField repeatField;

    public LoginView(Runnable onLoggedInCallback, ClientService service, I18n i18n, LocaleManager localeManager, Strings strings) {
        this.onLoggedInCallback = onLoggedInCallback;
        this.service = service;
        this.localeManager = localeManager;

        ChoiceBox<Locale> langBox = new ChoiceBox<>();
        for (Locale l : Strings.SUPPORTED) {
            langBox.getItems().add(l);
        }
        langBox.setValue(localeManager.getLocale());
        langBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Locale object) {
                if (object == null) return "";
                return strings.localeDisplayName(object);
            }
            @Override
            public Locale fromString(String string) {
                return null;
            }
        });
        langBox.valueProperty().addListener((o, a, b) -> {
            if (b != null) localeManager.setLocale(b);
        });

        Label langLabel = i18n.label("main.locale");
        HBox langRow = new HBox(8, langLabel, langBox);
        langRow.setAlignment(Pos.CENTER_RIGHT);

        Label welcome = new Label();
        welcome.textProperty().bind(i18n.bind("auth.welcome"));
        welcome.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        usernameField = new TextField();
        passwordField = new PasswordField();
        repeatField = new PasswordField();

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(i18n.label("auth.username"), 0, 0); grid.add(usernameField, 1, 0);
        grid.add(i18n.label("auth.password"), 0, 1); grid.add(passwordField, 1, 1);

        Label repeatLabel = i18n.label("auth.repeat");
        grid.add(repeatLabel, 0, 2); grid.add(repeatField, 1, 2);
        repeatLabel.setVisible(false); repeatField.setVisible(false);
        repeatLabel.setManaged(false); repeatField.setManaged(false);

        Button loginBtn = i18n.button("auth.login");
        Button registerBtn = i18n.button("auth.register");

        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> doAuth(false));
        registerBtn.setOnAction(e -> {
            if (!repeatField.isVisible()) {
                repeatLabel.setVisible(true); repeatField.setVisible(true);
                repeatLabel.setManaged(true); repeatField.setManaged(true);
                loginBtn.setDefaultButton(false);
                registerBtn.setDefaultButton(true);
            } else {
                doAuth(true);
            }
        });

        HBox actions = new HBox(10, loginBtn, registerBtn);
        actions.setAlignment(Pos.CENTER);

        root = new VBox(15, langRow, welcome, grid, actions);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
    }

    private void doAuth(boolean isRegister) {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (isRegister) {
            String repeat = repeatField.getText();
            if (!Objects.equals(pass, repeat)) {
                error(localeManager.t("auth.passwords_no_match"));
                return;
            }
        }

        try {
            Response response;
            if (isRegister) {
                response = service.register(user, pass);
            } else {
                response = service.login(user, pass);
            }

            if (response.getType() == ResponseType.AUTH_SUCCESS) {
                onLoggedInCallback.run();
            } else {
                service.close();
                String responseMessage = response.getMessage();
                if (responseMessage == null) {
                    error(localeManager.t("auth.error"));
                } else {
                    error(responseMessage);
                }
            }
        } catch (Exception e) {
            logger.error("Exception", e);
            error(e.getMessage());
        }
    }

    private void error(String text) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(localeManager.t("auth.error"));
        a.setContentText(text);
        a.showAndWait();
    }

    public VBox getRoot() { return root; }
}
