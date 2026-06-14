package pl.put.poznan.buildinginfo.gui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import pl.put.poznan.buildinginfo.model.Level;
import pl.put.poznan.buildinginfo.model.Room;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WorkspacePane extends VBox {

    private final AppState state;
    private final ApiClient api;
    private final ObjectMapper mapper = new ObjectMapper();

    // Dynamic sections rebuilt on each refresh
    private final Label titleLabel = new Label();
    private final HBox tabBar = new HBox(0);
    private final VBox body = new VBox(12);

    public WorkspacePane(AppState state, ApiClient api) {
        this.state = state;
        this.api = api;

        setStyle("-fx-background-color: #fafbfc;");
        setPadding(new Insets(0));
        VBox.setVgrow(this, Priority.ALWAYS);

        getChildren().addAll(buildTitleStrip(), buildTabBar(), buildBodyScroll());

        state.dirtyProperty().addListener((obs, o, n) -> rebuild());
        rebuild();
    }

    // ── Static structure ─────────────────────────────────────────

    private VBox buildTitleStrip() {
        Label eyebrow = new Label("QUERY");
        eyebrow.getStyleClass().add("eyebrow");
        eyebrow.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c8497; -fx-font-family: 'IBM Plex Mono';");

        titleLabel.setStyle("-fx-font-size: 19px; -fx-font-weight: 600; -fx-text-fill: #1f2531;");

        VBox strip = new VBox(4, eyebrow, titleLabel);
        strip.setPadding(new Insets(20, 24, 14, 24));
        strip.setStyle("-fx-background-color: #fafbfc; -fx-border-color: transparent transparent #dde0e6 transparent; -fx-border-width: 0 0 1 0;");
        return strip;
    }

    private HBox buildTabBar() {
        tabBar.setPadding(new Insets(0, 24, 0, 24));
        tabBar.setStyle("-fx-background-color: #fafbfc; -fx-border-color: transparent transparent #dde0e6 transparent; -fx-border-width: 0 0 1 0;");
        return tabBar;
    }

    private ScrollPane buildBodyScroll() {
        body.setPadding(new Insets(16, 24, 24, 24));
        body.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #fafbfc; -fx-background-color: #fafbfc;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    // ── Rebuild ──────────────────────────────────────────────────

    private void rebuild() {
        AppState.Endpoint ep = state.getEndpoint();

        // Title
        titleLabel.setText(switch (ep) {
            case AREA     -> "Total Area";
            case CUBE     -> "Total Volume";
            case LIGHT    -> "Lighting Density";
            case HEATING  -> "Heating Intensity";
            case EXCEEDED -> "Rooms Exceeding Threshold";
        });

        // Tabs
        tabBar.getChildren().clear();
        record Tab(String label, AppState.Endpoint ep) {}
        List<Tab> tabs = List.of(
                new Tab("▢  Area", AppState.Endpoint.AREA),
                new Tab("◫  Volume", AppState.Endpoint.CUBE),
                new Tab("✦  Lighting", AppState.Endpoint.LIGHT),
                new Tab("≋  Heating", AppState.Endpoint.HEATING),
                new Tab("△  Heating threshold", AppState.Endpoint.EXCEEDED)
        );
        for (Tab t : tabs) {
            Button btn = new Button(t.label());
            boolean active = t.ep() == ep;
            btn.setStyle(active
                    ? "-fx-background-color: transparent; -fx-text-fill: #1f2531; -fx-font-size: 12.5px; -fx-font-weight: 500; -fx-border-color: transparent transparent #1f2531 transparent; -fx-border-width: 0 0 2 0; -fx-padding: 10 14 10 14; -fx-cursor: hand;"
                    : "-fx-background-color: transparent; -fx-text-fill: #7c8497; -fx-font-size: 12.5px; -fx-border-color: transparent; -fx-padding: 10 14 10 14; -fx-cursor: hand;");
            btn.setOnAction(e -> {
                state.setEndpoint(t.ep());
                state.markDirty();
                fireApiCall();
            });
            tabBar.getChildren().add(btn);
        }

        // Body
        body.getChildren().clear();

        if (ep != AppState.Endpoint.EXCEEDED) {
            body.getChildren().add(buildScopeRow());
        }

        body.getChildren().add(buildUrlLine());
        body.getChildren().add(buildHeroCard());

        if (ep == AppState.Endpoint.EXCEEDED) {
            body.getChildren().add(buildThresholdPanel());
            body.getChildren().add(buildResultsPanel());
        } else {
            body.getChildren().add(buildComparisonPanel());
        }

        body.getChildren().add(buildResponsePanel());
    }

    // ── Scope row ────────────────────────────────────────────────

    private HBox buildScopeRow() {
        Label eyebrow = new Label("SCOPE");
        eyebrow.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #7c8497; -fx-font-family: 'IBM Plex Mono'; -fx-padding: 0 10 0 0;");

        AppState.Scope scope = state.getScope();
        ToggleGroup tg = new ToggleGroup();

        ToggleButton whole = pill("Whole building", tg, scope instanceof AppState.Scope.Whole);
        whole.setOnAction(e -> {
            state.setScope(new AppState.Scope.Whole());
            state.markDirty();
            fireApiCall();
        });

        // Level pill + combo
        boolean lvlActive = scope instanceof AppState.Scope.Lvl;
        ToggleButton lvlPill = pill("Level", tg, lvlActive);

        ComboBox<Level> lvlCombo = new ComboBox<>();
        lvlCombo.getItems().addAll(state.getBuilding().getLevels());
        lvlCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Level l) { return l == null ? "" : l.getName() + " · #" + l.getId(); }
            public Level fromString(String s) { return null; }
        });
        lvlCombo.setVisible(lvlActive);
        lvlCombo.setManaged(lvlActive);
        lvlCombo.setStyle("-fx-font-size: 12px;");
        if (lvlActive) {
            int lid = ((AppState.Scope.Lvl) scope).levelId();
            state.getBuilding().getLevels().stream().filter(l -> l.getId() == lid).findFirst().ifPresent(lvlCombo::setValue);
        }
        lvlCombo.setOnAction(e -> {
            Level sel = lvlCombo.getValue();
            if (sel != null) {
                state.setScope(new AppState.Scope.Lvl(sel.getId()));
                state.markDirty();
                fireApiCall();
            }
        });
        lvlPill.setOnAction(e -> {
            lvlCombo.setVisible(true);
            lvlCombo.setManaged(true);
            if (!state.getBuilding().getLevels().isEmpty()) {
                Level first = state.getBuilding().getLevels().get(0);
                lvlCombo.setValue(first);
                state.setScope(new AppState.Scope.Lvl(first.getId()));
                state.markDirty();
                fireApiCall();
            }
        });

        // Room pill + combo
        boolean rmActive = scope instanceof AppState.Scope.Rm;
        ToggleButton rmPill = pill("Room", tg, rmActive);

        ComboBox<Room> rmCombo = new ComboBox<>();
        List<Room> allRooms = new ArrayList<>();
        for (Level l : state.getBuilding().getLevels()) allRooms.addAll(l.getRooms());
        rmCombo.getItems().addAll(allRooms);
        rmCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Room r) { return r == null ? "" : r.getName() + " · #" + r.getId(); }
            public Room fromString(String s) { return null; }
        });
        rmCombo.setVisible(rmActive);
        rmCombo.setManaged(rmActive);
        rmCombo.setStyle("-fx-font-size: 12px;");
        if (rmActive) {
            int rid = ((AppState.Scope.Rm) scope).roomId();
            allRooms.stream().filter(r -> r.getId() == rid).findFirst().ifPresent(rmCombo::setValue);
        }
        rmCombo.setOnAction(e -> {
            Room sel = rmCombo.getValue();
            if (sel != null) {
                int levelId = state.getBuilding().getLevels().stream()
                        .filter(l -> l.getRooms().contains(sel)).mapToInt(Level::getId).findFirst().orElse(0);
                state.setScope(new AppState.Scope.Rm(sel.getId(), levelId));
                state.markDirty();
                fireApiCall();
            }
        });
        rmPill.setOnAction(e -> {
            rmCombo.setVisible(true);
            rmCombo.setManaged(true);
            if (!allRooms.isEmpty()) {
                Room first = allRooms.get(0);
                rmCombo.setValue(first);
                int levelId = state.getBuilding().getLevels().stream()
                        .filter(l -> l.getRooms().contains(first)).mapToInt(Level::getId).findFirst().orElse(0);
                state.setScope(new AppState.Scope.Rm(first.getId(), levelId));
                state.markDirty();
                fireApiCall();
            }
        });

        HBox row = new HBox(6, eyebrow, whole, lvlPill, lvlCombo, rmPill, rmCombo);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private ToggleButton pill(String text, ToggleGroup tg, boolean active) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(tg);
        btn.setSelected(active);
        String activeStyle = "-fx-background-color: #1f2531; -fx-text-fill: white; -fx-background-radius: 999; -fx-font-size: 12px; -fx-padding: 4 12 4 12; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: #eef0f3; -fx-text-fill: #56607a; -fx-background-radius: 999; -fx-font-size: 12px; -fx-padding: 4 12 4 12; -fx-cursor: hand;";
        btn.setStyle(active ? activeStyle : inactiveStyle);
        btn.selectedProperty().addListener((obs, o, n) -> btn.setStyle(n ? activeStyle : inactiveStyle));
        return btn;
    }

    // ── URL line ─────────────────────────────────────────────────

    private HBox buildUrlLine() {
        AppState.Endpoint ep = state.getEndpoint();
        AppState.Scope scope = state.getScope();

        String method = "POST";
        String path = buildPath(ep, scope);

        Label methodBadge = new Label(method);
        methodBadge.setStyle("-fx-background-color: #1f2531; -fx-text-fill: white; -fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-padding: 2 6 2 6; -fx-background-radius: 4;");

        Label pathLabel = new Label(path);
        pathLabel.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 12px; -fx-text-fill: #1f2531;");

        HBox card = new HBox(8, methodBadge, pathLabel);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 12, 8, 12));
        card.setStyle("-fx-background-color: white; -fx-border-color: #dde0e6; -fx-border-radius: 8; -fx-background-radius: 8;");

        if (ep == AppState.Endpoint.EXCEEDED) {
            Label threshold = new Label("?threshold=" + state.getThreshold());
            threshold.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 12px; -fx-text-fill: #d68b3a;");
            card.getChildren().add(threshold);
        }

        return card;
    }

    private String buildPath(AppState.Endpoint ep, AppState.Scope scope) {
        if (ep == AppState.Endpoint.EXCEEDED) return "/api/building/heating/exceeded";
        String metric = switch (ep) {
            case AREA    -> "area";
            case CUBE    -> "cube";
            case LIGHT   -> "light";
            case HEATING -> "heating";
            default      -> "area";
        };
        return switch (scope) {
            case AppState.Scope.Whole w -> "/api/building/" + metric;
            case AppState.Scope.Lvl l  -> "/api/building/level/" + l.levelId() + "/" + metric;
            case AppState.Scope.Rm r   -> "/api/building/room/" + r.roomId() + "/" + metric;
        };
    }

    // ── Hero card ────────────────────────────────────────────────

    private HBox buildHeroCard() {
        AppState.Endpoint ep = state.getEndpoint();
        String raw = state.getLastResponse();

        String labelText = "";
        String unit = "";
        String formula = "";
        double value = Double.NaN;
        boolean isAlert = false;
        String heroText = "—";

        switch (ep) {
            case AREA -> {
                labelText = "Area (total)"; unit = "m²"; formula = "Σ area";
                value = extractDouble(raw, "area");
            }
            case CUBE -> {
                labelText = "Volume (total)"; unit = "m³"; formula = "Σ cube";
                value = extractDouble(raw, "cube");
            }
            case LIGHT -> {
                labelText = "Lighting (avg W/m²)"; unit = "W/m²"; formula = "Σ light / Σ area";
                value = extractDouble(raw, "lightPerArea");
            }
            case HEATING -> {
                labelText = "Heating (avg kWh/m³)"; unit = "kWh/m³"; formula = "Σ heating / Σ cube";
                value = extractDouble(raw, "heatingPerCube");
                isAlert = !Double.isNaN(value) && value > 25;
            }
            case EXCEEDED -> {
                unit = ""; formula = "heating ÷ cube > " + state.getThreshold() + " kWh/m³";
                int count = countExceeded(raw);
                int total = state.getBuilding().getLevels().stream().mapToInt(l -> l.getRooms().size()).sum();
                heroText = count + " of " + total;
                labelText = "Rooms exceeding " + state.getThreshold() + " kWh/m³";
            }
        }

        if (ep != AppState.Endpoint.EXCEEDED && !Double.isNaN(value)) {
            heroText = fmt(value);
        }

        Label metricLabel = new Label(labelText.toUpperCase());
        metricLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c8497; -fx-font-family: 'IBM Plex Mono';");

        Label number = new Label(state.isLoading() ? "…" : heroText);
        number.setStyle("-fx-font-size: 52px; -fx-font-weight: 400; -fx-text-fill: " +
                (isAlert ? "#d68b3a" : "#56607a") + "; -fx-font-family: 'IBM Plex Sans';");

        Label unitLabel = new Label(unit);
        unitLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #7c8497; -fx-padding: 0 0 8 6;");
        HBox.setHgrow(unitLabel, Priority.NEVER);

        HBox numRow = new HBox(0, number, unitLabel);
        numRow.setAlignment(Pos.BASELINE_LEFT);

        VBox left = new VBox(4, metricLabel, numRow);
        left.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label formulaLabel = new Label(formula);
        formulaLabel.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11.5px; -fx-text-fill: #7c8497;");
        VBox right = new VBox(formulaLabel);
        right.setAlignment(Pos.BOTTOM_RIGHT);

        HBox card = new HBox(16, left, right);
        card.setPadding(new Insets(16, 20, 16, 20));
        String border = isAlert ? "#f5d0a9" : "#dde0e6";
        card.setStyle("-fx-background-color: white; -fx-border-color: " + border + "; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);");
        return card;
    }

    // ── Comparison panel ──────────────────────────────────────────

    private VBox buildComparisonPanel() {
        AppState.Endpoint ep = state.getEndpoint();
        AppState.Scope scope = state.getScope();

        String unit = switch (ep) {
            case AREA    -> "m²";
            case CUBE    -> "m³";
            case LIGHT   -> "W/m²";
            case HEATING -> "kWh/m³";
            default      -> "";
        };

        String headerText;
        List<double[]> rows = new ArrayList<>(); // [id, value, isActive]
        List<String> rowNames = new ArrayList<>();

        if (scope instanceof AppState.Scope.Lvl lvlScope) {
            Level targetLevel = state.getBuilding().getLevels().stream()
                    .filter(l -> l.getId() == lvlScope.levelId()).findFirst().orElse(null);
            headerText = "ROOMS IN " + (targetLevel != null ? targetLevel.getName().toUpperCase() : "LEVEL");
            if (targetLevel != null) {
                for (Room r : targetLevel.getRooms()) {
                    rowNames.add(r.getName() != null ? r.getName() : "#" + r.getId());
                    rows.add(new double[]{r.getId(), getRoomMetric(r, ep), 0});
                }
            }
        } else {
            headerText = "PER LEVEL";
            int activeId = scope instanceof AppState.Scope.Lvl l ? l.levelId() : -1;
            for (Level lvl : state.getBuilding().getLevels()) {
                rowNames.add(lvl.getName() != null ? lvl.getName() : "#" + lvl.getId());
                rows.add(new double[]{lvl.getId(), getLevelMetric(lvl, ep), activeId == lvl.getId() ? 1 : 0});
            }
        }

        Label header = new Label(headerText + "  ·  " + unit);
        header.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c8497; -fx-font-family: 'IBM Plex Mono';");

        VBox panel = new VBox(8, header);

        double maxVal = rows.stream().mapToDouble(r -> r[1]).max().orElse(1);
        if (maxVal == 0) maxVal = 1;

        for (int i = 0; i < rows.size(); i++) {
            double[] row = rows.get(i);
            String name = rowNames.get(i);
            double val = row[1];
            boolean active = row[2] == 1;

            Label nameLabel = new Label(name);
            nameLabel.setPrefWidth(110);
            nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (active ? "#1f2531" : "#56607a") + ";");

            double barWidth = (val / maxVal) * 180;
            Rectangle bar = new Rectangle(Math.max(barWidth, 2), 10);
            bar.setFill(Color.web(active ? "#3d7da6" : "#3d7da699"));
            bar.setArcWidth(3);
            bar.setArcHeight(3);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label valLabel = new Label(fmt(val));
            valLabel.setPrefWidth(70);
            valLabel.setAlignment(Pos.CENTER_RIGHT);
            valLabel.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11.5px; -fx-text-fill: " + (active ? "#1f2531" : "#56607a") + "; -fx-font-weight: " + (active ? "600" : "400") + ";");

            HBox barRow = new HBox(8, nameLabel, bar, spacer, valLabel);
            barRow.setAlignment(Pos.CENTER_LEFT);
            panel.getChildren().add(barRow);
        }

        panel.setStyle("-fx-background-color: white; -fx-border-color: #dde0e6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 14 16 14 16;");
        return panel;
    }

    // ── Threshold panel ───────────────────────────────────────────

    private HBox buildThresholdPanel() {
        Label lbl = new Label("THRESHOLD");
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c8497; -fx-font-family: 'IBM Plex Mono';");

        Slider slider = new Slider(0, 60, state.getThreshold());
        slider.setBlockIncrement(1);
        slider.setMajorTickUnit(10);
        slider.setStyle("-fx-accent: #d68b3a;");
        HBox.setHgrow(slider, Priority.ALWAYS);

        Label readout = new Label(state.getThreshold() + " kWh/m³");
        readout.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 12px; -fx-text-fill: #d68b3a; -fx-min-width: 80;");

        slider.valueProperty().addListener((obs, o, n) -> {
            int v = (int) Math.round(n.doubleValue());
            state.thresholdProperty().set(v);
            readout.setText(v + " kWh/m³");
        });
        slider.setOnMouseReleased(e -> { state.markDirty(); fireApiCall(); });

        HBox box = new HBox(12, lbl, slider, readout);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle("-fx-background-color: white; -fx-border-color: #dde0e6; -fx-border-radius: 8; -fx-background-radius: 8;");
        return box;
    }

    // ── Results panel ─────────────────────────────────────────────

    private VBox buildResultsPanel() {
        List<RoomResult> results = parseExceeded(state.getLastResponse());

        Label header = new Label("RESULTS · " + results.size() + " rooms");
        header.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c8497; -fx-font-family: 'IBM Plex Mono';");

        VBox panel = new VBox(6, header);

        if (results.isEmpty()) {
            Label empty = new Label("No rooms exceed this threshold.");
            empty.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #7c8497;");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            panel.getChildren().add(empty);
        } else {
            double maxHpc = results.stream().mapToDouble(r -> r.hpc).max().orElse(1);
            for (int i = 0; i < results.size(); i++) {
                RoomResult r = results.get(i);
                Label idx = new Label(String.format("%02d", i + 1));
                idx.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11px; -fx-text-fill: #aab0bc; -fx-min-width: 26;");

                Label name = new Label(r.name);
                name.setStyle("-fx-font-size: 12px; -fx-text-fill: #1f2531;");
                Label sub = new Label("#" + r.id);
                sub.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c8497;");
                VBox nameBox = new VBox(1, name, sub);
                HBox.setHgrow(nameBox, Priority.ALWAYS);

                double barW = (r.hpc / maxHpc) * 140;
                Rectangle bar = new Rectangle(Math.max(barW, 2), 6);
                bar.setFill(Color.web("#d68b3a"));
                bar.setArcWidth(2);
                bar.setArcHeight(2);

                Label val = new Label(fmt(r.hpc));
                val.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11.5px; -fx-text-fill: #d68b3a; -fx-min-width: 60; -fx-alignment: center-right;");

                HBox row = new HBox(8, idx, nameBox, bar, val);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 0, 4, 0));
                row.setStyle("-fx-cursor: hand;");
                final int rid = r.id;
                final int rLevelId = r.levelId;
                row.setOnMouseClicked(e -> {
                    state.setEndpoint(AppState.Endpoint.HEATING);
                    state.setScope(new AppState.Scope.Rm(rid, rLevelId));
                    state.markDirty();
                    fireApiCall();
                });
                panel.getChildren().add(row);
            }
        }

        panel.setPadding(new Insets(14, 16, 14, 16));
        panel.setStyle("-fx-background-color: white; -fx-border-color: #dde0e6; -fx-border-radius: 8; -fx-background-radius: 8;");
        return panel;
    }

    // ── Response panel ────────────────────────────────────────────

    private VBox buildResponsePanel() {
        Label header = new Label("RESPONSE");
        header.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c8497; -fx-font-family: 'IBM Plex Mono';");

        String rightText = state.isLoading() ? "loading…" : (state.isOffline() ? "offline" : "application/json");
        Label right = new Label(rightText);
        right.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-text-fill: " +
                (state.isOffline() ? "#d68b3a" : "#7c8497") + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerRow = new HBox(spacer, header, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, right);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        // fix: proper header layout
        HBox hRow = new HBox();
        hRow.getChildren().addAll(header, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, right);
        hRow.setAlignment(Pos.CENTER_LEFT);

        TextArea ta = new TextArea(state.getLastResponse().isEmpty() ? "(no response yet)" : state.getLastResponse());
        ta.setEditable(false);
        ta.setPrefRowCount(8);
        ta.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11.5px; -fx-control-inner-background: #f3f4f6; -fx-text-fill: #1f2531;");
        VBox.setVgrow(ta, Priority.NEVER);

        VBox panel = new VBox(8, hRow, ta);
        panel.setPadding(new Insets(14, 16, 14, 16));
        panel.setStyle("-fx-background-color: white; -fx-border-color: #dde0e6; -fx-border-radius: 8; -fx-background-radius: 8;");
        return panel;
    }

    // ── API call trigger ─────────────────────────────────────────

    public void fireApiCall() {
        api.fireAndUpdate(state, () -> {});
    }

    // ── Helpers ───────────────────────────────────────────────────

    private double extractDouble(String json, String key) {
        try {
            JsonNode node = mapper.readTree(json);
            if (node.has(key)) return node.get(key).asDouble();
        } catch (Exception ignored) {}
        return Double.NaN;
    }

    private int countExceeded(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            if (node.isArray()) return node.size();
        } catch (Exception ignored) {}
        return 0;
    }

    record RoomResult(int id, int levelId, String name, double hpc) {}

    private List<RoomResult> parseExceeded(String json) {
        List<RoomResult> list = new ArrayList<>();
        try {
            JsonNode arr = mapper.readTree(json);
            if (!arr.isArray()) return list;
            for (JsonNode node : arr) {
                int id = node.path("id").asInt();
                String name = node.path("name").asText("Room #" + id);
                double hpc = 0;
                double cube = node.path("cube").asDouble();
                double heating = node.path("heating").asDouble();
                if (cube > 0) hpc = heating / cube;
                // find levelId from state
                int levelId = 0;
                for (Level l : state.getBuilding().getLevels()) {
                    if (l.getRooms().stream().anyMatch(r -> r.getId() == id)) {
                        levelId = l.getId();
                        break;
                    }
                }
                list.add(new RoomResult(id, levelId, name, hpc));
            }
            list.sort(Comparator.comparingDouble(RoomResult::hpc).reversed());
        } catch (Exception ignored) {}
        return list;
    }

    private double getLevelMetric(Level l, AppState.Endpoint ep) {
        return switch (ep) {
            case AREA    -> l.getArea();
            case CUBE    -> l.getCube();
            case LIGHT   -> l.getLightPerArea();
            case HEATING -> l.getHeatingPerCube();
            default      -> 0;
        };
    }

    private double getRoomMetric(Room r, AppState.Endpoint ep) {
        return switch (ep) {
            case AREA    -> r.getArea();
            case CUBE    -> r.getCube();
            case LIGHT   -> r.getLightPerArea();
            case HEATING -> r.getHeatingPerCube();
            default      -> 0;
        };
    }

    private String fmt(double v) {
        if (Double.isNaN(v)) return "—";
        if (v == Math.floor(v) && v < 1_000_000) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }
}
