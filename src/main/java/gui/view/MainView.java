package gui.view;

import gui.GUIClient;
import gui.locale.I18n;
import gui.locale.LocaleManager;
import gui.locale.Strings;
import gui.service.ClientService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import models.Product;
import network.Response;
import network.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainView {
    private static final Logger logger = LoggerFactory.getLogger(MainView.class);
    private static final long POLL_SECONDS = 4;

    private final Runnable onLoggedOutCallback;
    private final ClientService service;

    private final I18n i18n;
    private final LocaleManager localeManager;
    private final Strings strings;

    private final BorderPane root = new BorderPane();

    private final ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private final ObservableList<Product> tableData = FXCollections.observableArrayList();
    private final TableView<Product> table = new TableView<>(tableData);

    private final VisualizationPane visualizationPane;
    private final TextArea outputArea = new TextArea();

    private final ChoiceBox<String> filterCol = new ChoiceBox<>();
    private final TextField filterValue = new TextField();
    private final ChoiceBox<String> sortCol = new ChoiceBox<>();

    private final Label userLabel = new Label();
    private final Label statusLabel = new Label();

    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "gui-poller");
        t.setDaemon(true);
        return t;
    });

    public MainView(Runnable onLoggedOutCallback, ClientService service, VisualizationPane visualizationPane, I18n i18n, LocaleManager localeManager, Strings strings) {
        this.onLoggedOutCallback = onLoggedOutCallback;
        this.service = service;
        this.visualizationPane = visualizationPane;
        this.i18n = i18n;
        this.localeManager = localeManager;
        this.strings = strings;

        root.setTop(buildTopBar());
        SplitPane center = new SplitPane();
        center.getItems().addAll(buildLeftPane(), buildRightPane());
        center.setDividerPositions(0.55);
        root.setCenter(center);
        root.setBottom(buildBottomBar());

        runAsync(() -> {
            service.sync();
            refreshCollection();
        });

        poller.scheduleWithFixedDelay(this::refreshCollection, POLL_SECONDS, POLL_SECONDS, TimeUnit.SECONDS);

        // re-render when locale changes
        localeManager.localeProperty().addListener((o, a, b) -> rebuildTableColumns());
        rebuildTableColumns();
        applyFilterAndSort();
    }

    public BorderPane getRoot() {
        return root;
    }

    private HBox buildTopBar() {
        userLabel.textProperty().bind(i18n.bind(() ->
                localeManager.t("main.user") + ": " + service.getCurrentUsername()));

        ChoiceBox<Locale> langBox = new ChoiceBox<>();
        for (Locale l : Strings.SUPPORTED) {
            langBox.getItems().add(l);
        }
        langBox.setValue(localeManager.getLocale());
        langBox.setConverter(new StringConverter<Locale>() {
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

        Button logoutBtn = i18n.button("main.logout");
        logoutBtn.setOnAction(e -> {
            poller.shutdownNow();
            onLoggedOutCallback.run();
            // app.onLogout();
        });

        Button refreshBtn = i18n.button("main.refresh");
        refreshBtn.setOnAction(e -> runAsync(this::refreshCollection));

        HBox box = new HBox(12, userLabel, spacer(),
                i18n.label("main.locale"), langBox, refreshBtn, logoutBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8));
        return box;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private VBox buildLeftPane() {
        // Filter row
        filterCol.getItems().clear();
        filterCol.getItems().add(localeManager.t("main.any"));
        filterCol.getItems().addAll(columnKeys().stream().map(localeManager::t).toList());
        filterCol.getSelectionModel().selectFirst();
        filterValue.promptTextProperty().bind(i18n.bind("main.filter"));
        filterValue.textProperty().addListener((o,a,b) -> applyFilterAndSort());
        filterCol.valueProperty().addListener((o,a,b) -> applyFilterAndSort());

        sortCol.getItems().clear();
        sortCol.getItems().add(localeManager.t("main.any"));
        sortCol.getItems().addAll(columnKeys().stream().map(localeManager::t).toList());
        sortCol.getSelectionModel().selectFirst();
        sortCol.valueProperty().addListener((o,a,b) -> applyFilterAndSort());

        localeManager.localeProperty().addListener((o,a,b) -> {
            int fi = filterCol.getSelectionModel().getSelectedIndex();
            int si = sortCol.getSelectionModel().getSelectedIndex();
            filterCol.getItems().setAll(Stream.concat(
                    Stream.of(localeManager.t("main.any")),
                    columnKeys().stream().map(localeManager::t)).toList());
            sortCol.getItems().setAll(Stream.concat(
                    Stream.of(localeManager.t("main.any")),
                    columnKeys().stream().map(localeManager::t)).toList());
            filterCol.getSelectionModel().select(Math.max(0, fi));
            sortCol.getSelectionModel().select(Math.max(0, si));
        });

        HBox filterRow = new HBox(8,
                i18n.label("main.filter"), filterCol, filterValue,
                i18n.label("main.sort"), sortCol);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.setPadding(new Insets(8));

        // Table
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Product> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && row.getItem() != null) doEdit(row.getItem());
            });
            return row;
        });

        // Action row
        Button addBtn = i18n.button("main.add");
        Button editBtn = i18n.button("main.edit");
        Button delBtn = i18n.button("main.delete");
        Button clearBtn = i18n.button("main.clear");
        addBtn.setOnAction(e -> doAdd());
        editBtn.setOnAction(e -> {
            Product s = table.getSelectionModel().getSelectedItem();
            if (s != null) doEdit(s);
        });
        delBtn.setOnAction(e -> {
            Product s = table.getSelectionModel().getSelectedItem();
            if (s != null) doDelete(s);
        });
        clearBtn.setOnAction(e -> doClear());
        HBox actions = new HBox(8, addBtn, editBtn, delBtn, clearBtn);
        actions.setPadding(new Insets(8));

        // Commands row (free-form server commands)
        TextField cmdField = new TextField();
        cmdField.promptTextProperty().bind(i18n.bind("main.commands"));
        Button execBtn = i18n.button("main.execute");
        execBtn.setOnAction(e -> {
            String line = cmdField.getText() == null ? "" : cmdField.getText().trim();
            if (!line.isEmpty()) executeFreeformCommand(line);
        });
        Button infoBtn = i18n.button("main.info");
        infoBtn.setOnAction(e -> executeFreeformCommand("info"));
        Button showBtn = i18n.button("main.show");
        showBtn.setOnAction(e -> executeFreeformCommand("show"));
        Button sumBtn = i18n.button("main.sum");
        sumBtn.setOnAction(e -> executeFreeformCommand("sum_of_price"));
        Button avgBtn = i18n.button("main.avg");
        avgBtn.setOnAction(e -> executeFreeformCommand("average_of_price"));
        Button shuffleBtn = i18n.button("main.shuffle");
        shuffleBtn.setOnAction(e -> executeFreeformCommand("shuffle"));
        Button sortBtn = i18n.button("main.sort");
        sortBtn.setOnAction(e -> executeFreeformCommand("sort"));
        Button filterStartsBtn = i18n.button("main.filter_starts");
        filterStartsBtn.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog();
            d.setHeaderText(localeManager.t("main.starts_with"));
            d.showAndWait().ifPresent(p -> {
                if (!p.isBlank()) executeFreeformCommand("filter_starts_with_name " + p);
            });
        });

        HBox cmdRow1 = new HBox(8, cmdField, execBtn);
        HBox.setHgrow(cmdField, Priority.ALWAYS);
        HBox cmdRow2 = new HBox(8, infoBtn, showBtn, sumBtn, avgBtn, shuffleBtn, sortBtn, filterStartsBtn);
        VBox cmdBox = new VBox(6, cmdRow1, cmdRow2);
        cmdBox.setPadding(new Insets(0, 8, 8, 8));

        outputArea.setEditable(false);
        outputArea.setPrefRowCount(6);

        VBox left = new VBox(filterRow, table, actions, cmdBox, outputArea);
        VBox.setVgrow(table, Priority.ALWAYS);
        return left;
    }

    private VBox buildRightPane() {
        Label title = i18n.label("vis.title");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label hint = i18n.label("vis.click_hint");
        hint.setStyle("-fx-text-fill: #707880;");
        VBox.setVgrow(visualizationPane, Priority.ALWAYS);
        visualizationPane.setOnProductClicked(this::showInfoDialog);
        VBox box = new VBox(6, title, hint, visualizationPane);
        box.setPadding(new Insets(8));
        VBox.setVgrow(visualizationPane, Priority.ALWAYS);
        return box;
    }

    private HBox buildBottomBar() {
        statusLabel.textProperty().bind(i18n.bind(() ->
                localeManager.t("status.connected") + " " + GUIClient.defaultHost + ":" + GUIClient.defaultPort
                        + " · " + localeManager.t("status.polling") + " " + POLL_SECONDS + " " + localeManager.t("status.seconds")));
        HBox box = new HBox(statusLabel);
        box.setPadding(new Insets(6, 10, 6, 10));
        box.setStyle("-fx-background-color: #eef1f4;");
        return box;
    }

    private List<String> columnKeys() {
        return List.of("col.id", "col.name", "col.x", "col.y", "col.creation",
                "col.price", "col.unit", "col.owner", "col.birthday", "col.height", "col.userId");
    }

    private void rebuildTableColumns() {
        table.getColumns().clear();
        TableColumn<Product, String> idCol = simple("col.id", p -> p.getId() == null ? "" : p.getId().toString());
        TableColumn<Product, String> nameCol = simple("col.name", Product::getName);
        TableColumn<Product, String> xCol = simple("col.x", p -> p.getCoordinates() == null ? "" : localeManager.formatNumber(p.getCoordinates().getX()));
        TableColumn<Product, String> yCol = simple("col.y", p -> p.getCoordinates() == null ? "" : localeManager.formatNumber(p.getCoordinates().getY()));
        TableColumn<Product, String> dateCol = simple("col.creation", p -> {
            try {
                return localeManager.formatDateTime(getCreationDate(p));
            }
            catch (Exception e) {
                return "";
            }
        });
        TableColumn<Product, String> priceCol = simple("col.price", p -> localeManager.formatPrice(p.getPrice()));
        TableColumn<Product, String> unitCol = simple("col.unit", p -> p.getUnitOfMeasure() == null ? "" : p.getUnitOfMeasure().name());
        TableColumn<Product, String> ownerCol = simple("col.owner", p -> p.getOwner() == null ? "" : p.getOwner().getName());
        TableColumn<Product, String> birthCol = simple("col.birthday", p -> {
            if (p.getOwner() == null) return "";
            LocalDate b = p.getOwner().getBirthday();
            return localeManager.formatDate(b);
        });
        TableColumn<Product, String> heightCol = simple("col.height", p -> p.getOwner() == null || p.getOwner().getHeight() == null ? "" : localeManager.formatNumber(p.getOwner().getHeight()));
        TableColumn<Product, String> uidCol = simple("col.userId", p -> String.valueOf(p.getUserId()));

        table.getColumns().addAll(idCol, nameCol, xCol, yCol, dateCol, priceCol, unitCol, ownerCol, birthCol, heightCol, uidCol);
    }

    private static Date getCreationDate(Product p) {
        try {
            Field f = Product.class.getDeclaredField("creationDate");
            f.setAccessible(true);
            return (Date) f.get(p);
        } catch (Exception e) {
            return null;
        }
    }

    private TableColumn<Product, String> simple(String key, Function<Product, String> getter) {
        TableColumn<Product, String> c = new TableColumn<>();
        c.textProperty().bind(i18n.bind(key));
        c.setCellValueFactory(d -> {
            String v = getter.apply(d.getValue());
            return new SimpleStringProperty(v == null ? "" : v);
        });
        c.setSortable(true);
        return c;
    }

    private void applyFilterAndSort() {
        String filterColName = filterCol.getValue();
        String filterText = filterValue.getText() == null ? "" : filterValue.getText().trim().toLowerCase(Locale.ROOT);
        String sortColName = sortCol.getValue();

        Function<Product, String> extractor = p -> "";
        Function<Product, Comparable<?>> sortKey = Product::getId;

        List<String> keys = columnKeys();
        for (String k : keys) {
            String localized = localeManager.t(k);
            if (Objects.equals(filterColName, localized)) extractor = colExtractor(k);
            if (Objects.equals(sortColName, localized)) sortKey = colSortKey(k);
        }

        Function<Product, String> extractorFinal = extractor;
        Function<Product, Comparable<?>> sortKeyFinal = sortKey;

        Comparator<Product> cmp = (a, b) -> {
            Comparable ca = sortKeyFinal.apply(a);
            Comparable cb = sortKeyFinal.apply(b);
            if (ca == null && cb == null) return 0;
            if (ca == null) return -1;
            if (cb == null) return 1;
            try {
                return ca.compareTo(cb);
            } catch (Exception e) {
                return 0;
            }
        };

        List<Product> result = allProducts.stream()
                .filter(p -> {
                    if (filterText.isEmpty()) return true;
                    if (Objects.equals(filterColName, localeManager.t("main.any")) || filterColName == null) {
                        return keys.stream()
                                .map(k -> colExtractor(k).apply(p))
                                .filter(Objects::nonNull)
                                .anyMatch(s -> s.toLowerCase(Locale.ROOT).contains(filterText));
                    }
                    String v = extractorFinal.apply(p);
                    return v != null && v.toLowerCase(Locale.ROOT).contains(filterText);
                })
                .sorted(cmp)
                .collect(Collectors.toList());

        Platform.runLater(() -> {
            tableData.setAll(result);
            visualizationPane.setProducts(result);
        });
    }

    private Function<Product, String> colExtractor(String key) {
        return switch (key) {
            case "col.id" -> p -> p.getId() == null ? "" : p.getId().toString();
            case "col.name" -> Product::getName;
            case "col.x" -> p -> p.getCoordinates() == null ? "" : String.valueOf(p.getCoordinates().getX());
            case "col.y" -> p -> p.getCoordinates() == null ? "" : String.valueOf(p.getCoordinates().getY());
            case "col.creation" -> p -> { Date d = getCreationDate(p); return d == null ? "" : d.toString(); };
            case "col.price" -> p -> String.valueOf(p.getPrice());
            case "col.unit" -> p -> p.getUnitOfMeasure() == null ? "" : p.getUnitOfMeasure().name();
            case "col.owner" -> p -> p.getOwner() == null ? "" : p.getOwner().getName();
            case "col.birthday" -> p -> p.getOwner() == null || p.getOwner().getBirthday() == null ? "" : p.getOwner().getBirthday().toString();
            case "col.height" -> p -> p.getOwner() == null || p.getOwner().getHeight() == null ? "" : String.valueOf(p.getOwner().getHeight());
            case "col.userId" -> p -> String.valueOf(p.getUserId());
            default -> p -> "";
        };
    }

    private Function<Product, Comparable<?>> colSortKey(String key) {
        return switch (key) {
            case "col.name" -> Product::getName;
            case "col.x" -> p -> p.getCoordinates() == null ? null : p.getCoordinates().getX();
            case "col.y" -> p -> p.getCoordinates() == null ? null : p.getCoordinates().getY();
            case "col.creation" -> MainView::getCreationDate;
            case "col.price" -> Product::getPrice;
            case "col.unit" -> p -> p.getUnitOfMeasure() == null ? null : p.getUnitOfMeasure().name();
            case "col.owner" -> p -> p.getOwner() == null ? null : p.getOwner().getName();
            case "col.birthday" -> p -> p.getOwner() == null ? null : p.getOwner().getBirthday();
            case "col.height" -> p -> p.getOwner() == null ? null : p.getOwner().getHeight();
            case "col.userId" -> Product::getUserId;
            default -> Product::getId;
        };
    }

    private void runAsync(Runnable r) {
        Thread t = new Thread(r, "gui-bg");
        t.setDaemon(true);
        t.start();
    }

    private void refreshCollection() {
        try {
            Response response = service.sendCommand("gshow", List.of(), List.of(), List.of());
            if (response.getType() == ResponseType.AUTH_REQUIRED) {
                Platform.runLater(this::handleAuthLost);
                return;
            }

            List<Product> products = response.getProducts();
            Platform.runLater(() -> {
                allProducts.setAll(products);
                applyFilterAndSort();
            });
        } catch (Exception e) {
            logger.warn("refresh failed", e);
        }
    }

    private void handleAuthLost() {
        poller.shutdownNow();
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(localeManager.t("auth.error"));
        a.showAndWait();
        onLoggedOutCallback.run();
    }

    private void doAdd() {
        Optional<Product> p = new ProductDialog((Stage) root.getScene().getWindow(), null, i18n, localeManager).showAndWait();
        p.ifPresent(prod -> runAsync(() -> {
            Response r = service.sendCommand("add", List.of(), List.of(), List.of(prod));
            appendOutput(r.getMessage());
            refreshCollection();
        }));
    }

    private void doEdit(Product existing) {
        Optional<Product> p = new ProductDialog((Stage) root.getScene().getWindow(), existing, i18n, localeManager).showAndWait();
        p.ifPresent(prod -> runAsync(() -> {
            Response r = service.sendCommand("update", List.of(), List.of(existing.getId()), List.of(prod));
            appendOutput(r.getMessage());
            refreshCollection();
        }));
    }

    private void doDelete(Product existing) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                localeManager.t("dialog.confirm_delete"),
                ButtonType.YES, ButtonType.NO);
        a.setHeaderText(localeManager.t("dialog.confirm"));
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.YES) {
            runAsync(() -> {
                Response resp = service.sendCommand("remove_by_id", List.of(), List.of(existing.getId()), List.of());
                appendOutput(resp.getMessage());
                refreshCollection();
            });
        }
    }

    private void doClear() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                localeManager.t("main.clear"),
                ButtonType.YES, ButtonType.NO);
        a.setHeaderText(localeManager.t("dialog.confirm"));
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.YES) {
            runAsync(() -> {
                Response resp = service.sendCommand("clear", List.of(), List.of(), List.of());
                appendOutput(resp.getMessage());
                refreshCollection();
            });
        }
    }

    private void executeFreeformCommand(String line) {
        String[] tokens = line.split("\\s+");
        String name = tokens[0];
        List<String> stringArgs = new ArrayList<>();
        List<Integer> intArgs = new ArrayList<>();
        for (int i = 1; i < tokens.length; i++) {
            try { intArgs.add(Integer.parseInt(tokens[i])); }
            catch (NumberFormatException ex) { stringArgs.add(tokens[i]); }
        }
        runAsync(() -> {
            Response r = service.sendCommand(name, stringArgs, intArgs, List.of());
            appendOutput("> " + line + "\n" + (r.getMessage() == null ? "" : r.getMessage()));
            if (r.getProducts() != null) {
                Platform.runLater(() -> {
                    allProducts.setAll(r.getProducts());
                    applyFilterAndSort();
                });
            } else {
                refreshCollection();
            }
        });
    }

    private void appendOutput(String text) {
        if (text == null) return;
        Platform.runLater(() -> {
            outputArea.appendText(text);
            outputArea.appendText("\n");
        });
    }

    private void showInfoDialog(Product p) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(localeManager.t("col.name") + ": " + p.getName());
        StringBuilder sb = new StringBuilder();
        sb.append(localeManager.t("col.id")).append(": ").append(p.getId()).append('\n');
        if (p.getCoordinates() != null) {
            sb.append(localeManager.t("col.x")).append(": ").append(localeManager.formatNumber(p.getCoordinates().getX())).append('\n');
            sb.append(localeManager.t("col.y")).append(": ").append(localeManager.formatNumber(p.getCoordinates().getY())).append('\n');
        }
        sb.append(localeManager.t("col.price")).append(": ").append(localeManager.formatPrice(p.getPrice())).append('\n');
        if (p.getUnitOfMeasure() != null)
            sb.append(localeManager.t("col.unit")).append(": ").append(p.getUnitOfMeasure()).append('\n');
        Date d = getCreationDate(p);
        if (d != null) sb.append(localeManager.t("col.creation")).append(": ").append(localeManager.formatDateTime(d)).append('\n');
        if (p.getOwner() != null) {
            sb.append(localeManager.t("col.owner")).append(": ").append(p.getOwner().getName()).append('\n');
            if (p.getOwner().getBirthday() != null)
                sb.append(localeManager.t("col.birthday")).append(": ").append(localeManager.formatDate(p.getOwner().getBirthday())).append('\n');
            if (p.getOwner().getHeight() != null)
                sb.append(localeManager.t("col.height")).append(": ").append(localeManager.formatNumber(p.getOwner().getHeight())).append('\n');
        }
        sb.append(localeManager.t("col.userId")).append(": ").append(p.getUserId());
        a.setContentText(sb.toString());

        ButtonType edit = new ButtonType(localeManager.t("main.edit"));
        ButtonType del = new ButtonType(localeManager.t("main.delete"));
        a.getButtonTypes().setAll(edit, del, ButtonType.CLOSE);
        Optional<ButtonType> bt = a.showAndWait();
        if (bt.isPresent()) {
            if (bt.get() == edit) doEdit(p);
            else if (bt.get() == del) doDelete(p);
        }
    }
}
