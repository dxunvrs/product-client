package gui.view;

import gui.locale.I18n;
import gui.locale.LocaleManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Coordinates;
import models.Person;
import models.Product;
import models.UnitOfMeasure;

import java.time.LocalDate;
import java.util.Optional;

public class ProductDialog {
    private final Stage stage = new Stage();

    private final TextField nameField = new TextField();
    private final TextField xField = new TextField();
    private final TextField yField = new TextField();
    private final TextField priceField = new TextField();
    private final ChoiceBox<UnitOfMeasure> unitBox = new ChoiceBox<>();
    private final TextField ownerNameField = new TextField();
    private final DatePicker birthdayPicker = new DatePicker();
    private final TextField heightField = new TextField();

    private Product result;

    private final I18n i18n;
    private final LocaleManager localeManager;

    public ProductDialog(Stage owner, Product existing, I18n i18n, LocaleManager localeManager) {
        this.i18n = i18n;
        this.localeManager = localeManager;

        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        i18n.bindStageTitle(stage, existing == null ? "form.add_title" : "form.edit_title");

        unitBox.getItems().addAll(UnitOfMeasure.values());

        if (existing != null) {
            nameField.setText(existing.getName());
            if (existing.getCoordinates() != null) {
                xField.setText(String.valueOf(existing.getCoordinates().getX()));
                yField.setText(String.valueOf(existing.getCoordinates().getY()));
            }
            priceField.setText(String.valueOf(existing.getPrice()));
            unitBox.setValue(existing.getUnitOfMeasure());
            if (existing.getOwner() != null) {
                ownerNameField.setText(existing.getOwner().getName());
                birthdayPicker.setValue(existing.getOwner().getBirthday());
                if (existing.getOwner().getHeight() != null) heightField.setText(String.valueOf(existing.getOwner().getHeight()));
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new Insets(15));

        int r = 0;
        addRow(grid, r++, "col.name", nameField);
        addRow(grid, r++, "col.x", xField);
        addRow(grid, r++, "col.y", yField);
        addRow(grid, r++, "col.price", priceField);
        addRow(grid, r++, "col.unit", unitBox);
        addRow(grid, r++, "col.owner", ownerNameField);
        addRow(grid, r++, "col.birthday", birthdayPicker);
        addRow(grid, r++, "col.height", heightField);

        Button save = i18n.button("form.save");
        Button cancel = i18n.button("form.cancel");
        save.setDefaultButton(true);
        cancel.setCancelButton(true);
        save.setOnAction(e -> {
            try {
                Product p = build(existing);
                if (p == null) return;
                result = p;
                stage.close();
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });
        cancel.setOnAction(e -> stage.close());
        HBox actions = new HBox(10, save, cancel);
        actions.setPadding(new Insets(0, 15, 15, 15));

        VBox box = new VBox(grid, actions);
        stage.setScene(new Scene(box));
    }

    private void addRow(GridPane grid, int row, String key, javafx.scene.Node field) {
        Label l = i18n.label(key);
        grid.add(l, 0, row);
        grid.add(field, 1, row);
    }

    private Product build(Product existing) {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) { error(localeManager.t("form.invalid_name")); return null; }
        long x;
        try {
            x = Long.parseLong(xField.getText().trim());
        } catch (Exception e) {
            error(localeManager.t("form.invalid_x"));
            return null;
        }
        if (x <= -425) {
            error(localeManager.t("form.invalid_x"));
            return null;
        }
        int y;
        try {
            y = Integer.parseInt(yField.getText().trim());
        } catch (Exception e) {
            error(localeManager.t("form.invalid_y"));
            return null;
        }
        int price;
        try {
            price = Integer.parseInt(priceField.getText().trim());
        } catch (Exception e) {
            error(localeManager.t("form.invalid_price"));
            return null;
        }
        if (price <= 0) {
            error(localeManager.t("form.invalid_price"));
            return null;
        }
        UnitOfMeasure unit = unitBox.getValue();
        if (unit == null) {
            error(localeManager.t("form.invalid_unit"));
            return null;
        }
        String ownerName = ownerNameField.getText() == null ? "" : ownerNameField.getText().trim();
        if (ownerName.isEmpty()) {
            error(localeManager.t("form.invalid_owner_name"));
            return null;
        }
        LocalDate birthday = birthdayPicker.getValue();
        if (birthday == null) {
            error(localeManager.t("form.invalid_birthday"));
            return null;
        }
        long height;
        try {
            height = Long.parseLong(heightField.getText().trim());
        } catch (Exception e) {
            error(localeManager.t("form.invalid_height"));
            return null;
        }
        if (height <= 0) {
            error(localeManager.t("form.invalid_height"));
            return null;
        }

        return new Product(name, new Coordinates(x, y), price, unit, new Person(ownerName, birthday, height));
    }

    private void error(String text) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(localeManager.t("form.error"));
        a.setContentText(text);
        a.showAndWait();
    }

    public Optional<Product> showAndWait() {
        stage.showAndWait();
        return Optional.ofNullable(result);
    }
}
