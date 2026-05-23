package pl.put.poznan.buildinginfo.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pl.put.poznan.buildinginfo.model.Building;
import pl.put.poznan.buildinginfo.model.Level;
import pl.put.poznan.buildinginfo.model.Room;

public class ComposerPane extends VBox {

    private final AppState state;
    private final Runnable fireApi;
    private final VBox cards = new VBox(8);
    private final ObjectMapper mapper = new ObjectMapper();

    public ComposerPane(AppState state, Runnable fireApi) {
        this.state = state;
        this.fireApi = fireApi;

        setPrefWidth(390);
        setMinWidth(340);
        setStyle("-fx-background-color: #f3f4f6;");

        getChildren().addAll(buildHeader(), buildScrollArea());

        state.dirtyProperty().addListener((obs, o, n) -> rebuild());
        rebuild();
    }

    private VBox buildHeader() {
        Label eyebrow = new Label("COMPOSE REQUEST BODY");
        eyebrow.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c8497; -fx-font-family: 'IBM Plex Mono';");

        Label title = new Label("Building JSON");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #1f2531;");

        Button pasteBtn = new Button("⇩ Paste JSON");
        pasteBtn.setStyle("-fx-background-color: #1f2531; -fx-text-fill: white; -fx-font-size: 11.5px; " +
                "-fx-background-radius: 6; -fx-padding: 5 12 5 12; -fx-cursor: hand;");
        pasteBtn.setOnMouseEntered(e -> pasteBtn.setStyle("-fx-background-color: #2c3446; -fx-text-fill: white; -fx-font-size: 11.5px; -fx-background-radius: 6; -fx-padding: 5 12 5 12; -fx-cursor: hand;"));
        pasteBtn.setOnMouseExited(e -> pasteBtn.setStyle("-fx-background-color: #1f2531; -fx-text-fill: white; -fx-font-size: 11.5px; -fx-background-radius: 6; -fx-padding: 5 12 5 12; -fx-cursor: hand;"));
        pasteBtn.setOnAction(e -> showPasteDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleRow = new HBox(8, title, spacer, pasteBtn);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label desc = new Label("Every API call POSTs this whole tree. Edit inline, then switch endpoints.");
        desc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #7c8497; -fx-wrap-text: true;");
        desc.setMaxWidth(340);

        VBox header = new VBox(4, eyebrow, titleRow, desc);
        header.setPadding(new Insets(16, 18, 14, 18));
        header.setStyle("-fx-background-color: #f3f4f6; -fx-border-color: transparent transparent #dde0e6 transparent; -fx-border-width: 0 0 1 0;");
        return header;
    }

    private void showPasteDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Paste Building JSON");
        dialog.setHeaderText("Paste your building JSON and click Import.");

        TextArea ta = new TextArea();
        ta.setPromptText("{\n  \"id\": 1,\n  \"name\": \"My Building\",\n  \"levels\": [\n    {\n      \"id\": 10,\n      \"name\": \"Ground Floor\",\n      \"rooms\": [...]\n    }\n  ]\n}");
        ta.setPrefRowCount(18);
        ta.setPrefWidth(480);
        ta.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 12px;");

        VBox content = new VBox(8, ta);
        content.setPadding(new Insets(4, 0, 0, 0));
        dialog.getDialogPane().setContent(content);

        ButtonType importBtn = new ButtonType("Import", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(importBtn, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> bt == importBtn ? ta.getText().trim() : null);

        dialog.showAndWait().ifPresent(json -> {
            if (json.isEmpty()) return;
            try {
                Building parsed = mapper.readValue(json, Building.class);
                state.setBuilding(parsed);
                state.setScope(new AppState.Scope.Whole());
                state.setLastResponse("");
                state.markDirty();
                fireApi.run();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Parse Error");
                alert.setHeaderText("Invalid JSON");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });
    }

    private ScrollPane buildScrollArea() {
        cards.setPadding(new Insets(12, 12, 12, 12));
        cards.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #f3f4f6; -fx-background-color: #f3f4f6;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private void rebuild() {
        cards.getChildren().clear();
        Building b = state.getBuilding();
        if (b == null) return;
        cards.getChildren().add(buildBuildingCard(b));
        for (Level lvl : b.getLevels()) {
            cards.getChildren().add(buildLevelCard(lvl));
        }
        if (b.getLevels().isEmpty()) {
            cards.getChildren().add(buildEmptyState());
        }
        cards.getChildren().add(buildAddLevelButton());
    }

    private VBox buildEmptyState() {
        Label msg = new Label("No levels yet.");
        msg.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #7c8497;");

        Label hint = new Label("Use \"Paste JSON\" above to import a building,\nor add a level manually below.");
        hint.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #aab0bc; -fx-wrap-text: true; -fx-text-alignment: center;");
        hint.setMaxWidth(300);

        VBox box = new VBox(6, msg, hint);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28, 16, 28, 16));
        box.setStyle("-fx-background-color: white; -fx-border-color: #dde0e6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-style: dashed;");
        return box;
    }

    private VBox buildBuildingCard(Building b) {
        // Header row
        Label glyph = new Label("▣");
        glyph.setStyle("-fx-font-size: 14px; -fx-text-fill: #1f2531;");

        TextField nameField = styledTextField(b.getName() != null ? b.getName() : "");
        nameField.setPromptText("Building name");
        nameField.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #1f2531;");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        commitOnChange(nameField, v -> { b.setName(v); state.markDirty(); });

        TextField idField = monoTextField(String.valueOf(b.getId()));
        idField.setPrefWidth(50);
        commitOnChange(idField, v -> {
            try { b.setId(Integer.parseInt(v.trim())); state.markDirty(); } catch (NumberFormatException ignored) {}
        });

        HBox header = new HBox(6, glyph, nameField, new Label("#"), idField);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 12, 8, 12));
        header.setStyle("-fx-background-color: #1f2531; -fx-background-radius: 8 8 0 0;");
        glyph.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
        nameField.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: white;");
        Label hashLabel = new Label("#");
        hashLabel.setStyle("-fx-text-fill: #7c8497;");
        idField.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11px; -fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #aab0bc;");
        HBox headerFixed = new HBox(6, glyph, nameField, hashLabel, idField);
        headerFixed.setAlignment(Pos.CENTER_LEFT);
        headerFixed.setPadding(new Insets(10, 12, 8, 12));
        headerFixed.setStyle("-fx-background-color: #1f2531; -fx-background-radius: 8 8 0 0;");

        // Totals strip
        GridPane totals = new GridPane();
        totals.setHgap(0);
        totals.setPadding(new Insets(8, 12, 8, 12));
        totals.setStyle("-fx-background-color: #eef0f3;");

        String[] labels = {"AREA", "CUBE", "LIGHT", "HEAT"};
        double[] values = {b.getArea(), b.getCube(), b.getLight(), b.getHeating()};
        String[] units = {"m²", "m³", "W", ""};

        for (int i = 0; i < 4; i++) {
            VBox col = new VBox(2);
            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #7c8497; -fx-font-family: 'IBM Plex Mono';");
            Label val = new Label(fmt(values[i]) + " " + units[i]);
            val.setStyle("-fx-font-size: 11px; -fx-text-fill: #1f2531; -fx-font-family: 'IBM Plex Mono'; -fx-font-weight: 500;");
            col.getChildren().addAll(lbl, val);
            col.setAlignment(Pos.TOP_LEFT);
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            totals.getColumnConstraints().add(cc);
            totals.add(col, i, 0);
        }

        VBox card = new VBox(headerFixed, totals);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dde0e6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1);");
        return card;
    }

    private VBox buildLevelCard(Level lvl) {
        Label glyph = new Label("▭");
        glyph.setStyle("-fx-font-size: 13px; -fx-text-fill: #56607a;");

        TextField nameField = styledTextField(lvl.getName() != null ? lvl.getName() : "");
        nameField.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #1f2531;");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        commitOnChange(nameField, v -> { lvl.setName(v); state.markDirty(); });

        TextField idField = monoTextField(String.valueOf(lvl.getId()));
        idField.setPrefWidth(45);
        commitOnChange(idField, v -> {
            try { lvl.setId(Integer.parseInt(v.trim())); state.markDirty(); } catch (NumberFormatException ignored) {}
        });

        Label roomCount = new Label("· " + lvl.getRooms().size() + " rooms");
        roomCount.setStyle("-fx-font-size: 11px; -fx-text-fill: #7c8497;");

        Button deleteBtn = new Button("×");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7c8497; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        deleteBtn.setOnAction(e -> {
            state.getBuilding().getLevels().remove(lvl);
            if (state.getScope() instanceof AppState.Scope.Lvl sl && sl.levelId() == lvl.getId()) {
                state.setScope(new AppState.Scope.Whole());
            } else if (state.getScope() instanceof AppState.Scope.Rm sr && sr.levelId() == lvl.getId()) {
                state.setScope(new AppState.Scope.Whole());
            }
            state.markDirty();
            fireApi.run();
        });

        HBox levelHeader = new HBox(6, glyph, nameField, new Label("#") {{
            setStyle("-fx-text-fill: #aab0bc; -fx-font-size: 11px;");
        }}, idField, roomCount, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, deleteBtn);
        levelHeader.setAlignment(Pos.CENTER_LEFT);
        levelHeader.setPadding(new Insets(8, 10, 6, 12));
        levelHeader.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 8 8 0 0; -fx-border-color: transparent transparent #dde0e6 transparent; -fx-border-width: 0 0 1 0;");

        // Room table — one GridPane so all rows share the same ColumnConstraints
        // Columns: [cb:28][name:flex][area:60][cube:60][heat:60][light:60][del:28]
        GridPane roomGrid = new GridPane();
        roomGrid.getColumnConstraints().addAll(
            cc(28, HPos.CENTER), ccFlex(),
            cc(60, HPos.RIGHT), cc(60, HPos.RIGHT), cc(60, HPos.RIGHT), cc(60, HPos.RIGHT),
            cc(28, HPos.CENTER)
        );
        roomGrid.setPadding(new Insets(0, 8, 0, 8));

        addGridHeader(roomGrid, 0);
        for (int i = 0; i < lvl.getRooms().size(); i++) {
            addGridRoom(roomGrid, lvl.getRooms().get(i), lvl, i + 1);
        }

        Button addRoom = new Button("+ Add room");
        addRoom.setMaxWidth(Double.MAX_VALUE);
        addRoom.setStyle("-fx-background-color: #f9fafb; -fx-text-fill: #56607a; -fx-font-size: 11.5px; -fx-border-color: transparent; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        addRoom.setOnAction(e -> {
            int nextId = lvl.getRooms().stream().mapToInt(Room::getId).max().orElse(lvl.getId() * 10) + 1;
            lvl.addRoom(new Room(nextId, "New Room", 20, 60, 600, 200));
            state.markDirty();
        });

        VBox card = new VBox(levelHeader, roomGrid, addRoom);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dde0e6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 3, 0, 0, 1);");
        return card;
    }

    private void addGridHeader(GridPane grid, int row) {
        Pane bg = new Pane();
        bg.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        bg.setStyle("-fx-background-color: #f5f6f7; -fx-border-color: transparent transparent #dde0e6 transparent; -fx-border-width: 0 0 1 0;");
        GridPane.setColumnSpan(bg, GridPane.REMAINING);
        GridPane.setFillWidth(bg, true);
        GridPane.setFillHeight(bg, true);
        grid.add(bg, 0, row);

        String[] headers = {"", "ROOM", "AREA", "CUBE", "HEAT", "LIGHT", ""};
        for (int col = 0; col < headers.length; col++) {
            if (headers[col].isEmpty()) continue;
            Label lbl = new Label(headers[col]);
            lbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #7c8497; -fx-font-family: 'IBM Plex Mono';");
            lbl.setMaxWidth(Double.MAX_VALUE);
            GridPane.setMargin(lbl, new Insets(5, 4, 4, 4));
            grid.add(lbl, col, row);
        }
    }

    private void addGridRoom(GridPane grid, Room room, Level lvl, int row) {
        boolean sel = state.getScope() instanceof AppState.Scope.Rm sr && sr.roomId() == room.getId();

        Pane bg = new Pane();
        bg.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setColumnSpan(bg, GridPane.REMAINING);
        GridPane.setFillWidth(bg, true);
        GridPane.setFillHeight(bg, true);
        applyRowBg(bg, sel, false);
        bg.setOnMouseEntered(e -> applyRowBg(bg,
            state.getScope() instanceof AppState.Scope.Rm sr2 && sr2.roomId() == room.getId(), true));
        bg.setOnMouseExited(e -> applyRowBg(bg,
            state.getScope() instanceof AppState.Scope.Rm sr2 && sr2.roomId() == room.getId(), false));
        grid.add(bg, 0, row);

        CheckBox cb = new CheckBox();
        cb.setSelected(sel);
        cb.setOnAction(e -> {
            if (cb.isSelected()) state.setScope(new AppState.Scope.Rm(room.getId(), lvl.getId()));
            else state.setScope(new AppState.Scope.Whole());
            state.markDirty();
            fireApi.run();
        });
        GridPane.setMargin(cb, new Insets(5, 0, 5, 0));
        grid.add(cb, 0, row);

        TextField nameF = styledRoomField(room.getName() != null ? room.getName() : "");
        nameF.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(nameF, true);
        GridPane.setMargin(nameF, new Insets(2, 4, 2, 0));
        commitOnChange(nameF, v -> { room.setName(v); state.markDirty(); });
        grid.add(nameF, 1, row);

        String[] vals = {fmt(room.getArea()), fmt(room.getCube()), fmt(room.getHeating()), fmt(room.getLight())};
        for (int c = 0; c < 4; c++) {
            TextField tf = monoRoomField(vals[c]);
            tf.setMaxWidth(Double.MAX_VALUE);
            GridPane.setFillWidth(tf, true);
            GridPane.setMargin(tf, new Insets(2, 0, 2, 0));
            final int ci = c;
            commitOnChange(tf, v -> {
                try {
                    double val = Double.parseDouble(v.trim());
                    switch (ci) {
                        case 0 -> room.setArea(val);
                        case 1 -> room.setCube(val);
                        case 2 -> room.setHeating(val);
                        case 3 -> room.setLight(val);
                    }
                    state.markDirty();
                    fireApi.run();
                } catch (NumberFormatException ignored) {}
            });
            grid.add(tf, c + 2, row);
        }

        Button del = new Button("×");
        del.setStyle("-fx-background-color: transparent; -fx-text-fill: #aab0bc; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;");
        del.setOnAction(e -> {
            lvl.getRooms().remove(room);
            if (state.getScope() instanceof AppState.Scope.Rm sr && sr.roomId() == room.getId()) {
                state.setScope(new AppState.Scope.Lvl(lvl.getId()));
            }
            state.markDirty();
            fireApi.run();
        });
        GridPane.setMargin(del, new Insets(4, 0, 4, 0));
        grid.add(del, 6, row);
    }

    private void applyRowBg(Pane bg, boolean selected, boolean hover) {
        String color = selected ? "#eef4f9" : hover ? "#f7f8fa" : "white";
        bg.setStyle("-fx-background-color: " + color +
            "; -fx-border-color: transparent transparent #dde0e6 transparent; -fx-border-width: 0 0 1 0;");
    }

    private ColumnConstraints cc(double w, HPos align) {
        ColumnConstraints c = new ColumnConstraints(w, w, w);
        c.setHalignment(align);
        return c;
    }

    private ColumnConstraints ccFlex() {
        ColumnConstraints c = new ColumnConstraints();
        c.setHgrow(Priority.ALWAYS);
        c.setFillWidth(true);
        c.setMinWidth(60);
        return c;
    }

    private Button buildAddLevelButton() {
        Button btn = new Button("+ Add level");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7c8497; -fx-font-size: 12.5px; " +
                "-fx-border-color: #dde0e6; -fx-border-style: dashed; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-padding: 10 0 10 0; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #eef0f3; -fx-text-fill: #1f2531; -fx-font-size: 12.5px; " +
                "-fx-border-color: #aab0bc; -fx-border-style: dashed; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-padding: 10 0 10 0; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7c8497; -fx-font-size: 12.5px; " +
                "-fx-border-color: #dde0e6; -fx-border-style: dashed; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-padding: 10 0 10 0; -fx-cursor: hand;"));
        btn.setOnAction(e -> {
            Building b = state.getBuilding();
            int nextId = b.getLevels().stream().mapToInt(Level::getId).max().orElse(0) + 1;
            b.addLevel(new Level(nextId, "New Level"));
            state.markDirty();
        });
        return btn;
    }

    // ── Helpers ────────────────────────────────────────────────────

    private TextField styledTextField(String text) {
        TextField tf = new TextField(text);
        tf.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #1f2531;");
        return tf;
    }

    private TextField monoTextField(String text) {
        TextField tf = new TextField(text);
        tf.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11px; -fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #56607a;");
        return tf;
    }

    private TextField styledRoomField(String text) {
        TextField tf = new TextField(text);
        tf.setStyle("-fx-font-size: 12px; -fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #1f2531; -fx-padding: 0 4 0 4;");
        return tf;
    }

    private TextField monoRoomField(String text) {
        TextField tf = new TextField(text);
        tf.setAlignment(Pos.CENTER_RIGHT);
        tf.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11px; -fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #56607a; -fx-padding: 0 4 0 4;");
        return tf;
    }

    private void setFixed(Region r, double w) {
        r.setMinWidth(w); r.setPrefWidth(w); r.setMaxWidth(w);
    }

    private void commitOnChange(TextField tf, java.util.function.Consumer<String> handler) {
        tf.setOnAction(e -> handler.accept(tf.getText()));
        tf.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) handler.accept(tf.getText());
        });
    }

    private String fmt(double v) {
        if (v == Math.floor(v) && v < 1_000_000) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }
}
