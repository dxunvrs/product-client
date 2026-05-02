package gui.locale;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.concurrent.Callable;

public class I18n {
    private final LocaleManager localeManager;

    public I18n(LocaleManager localeManager) {
        this.localeManager = localeManager;
    }

    public StringBinding bind(String key) {
        Callable<String> compute = () -> localeManager.t(key);
        return Bindings.createStringBinding(compute, localeManager.localeProperty());
    }

    public StringBinding bind(Callable<String> compute) {
        return Bindings.createStringBinding(compute, localeManager.localeProperty());
    }

    public Label label(String key) {
        Label l = new Label();
        l.textProperty().bind(bind(key));
        return l;
    }

    public Button button(String key) {
        Button b = new Button();
        b.textProperty().bind(bind(key));
        return b;
    }

    public void bindStageTitle(Stage stage, String key) {
        stage.titleProperty().bind(bind(key));
    }
}