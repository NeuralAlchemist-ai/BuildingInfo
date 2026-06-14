package pl.put.poznan.buildinginfo.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.web.context.WebServerApplicationContext;
import pl.put.poznan.buildinginfo.app.BuildingInfoApplication;
import pl.put.poznan.buildinginfo.model.Building;

public class BuildingInfoGui extends Application {

    private ConfigurableApplicationContext springContext;
    private String apiBaseUrl = "http://localhost:8080";

    @Override
    public void init() {
        springContext = SpringApplication.run(
                BuildingInfoApplication.class,
                "--server.port=0",
                "--spring.main.web-application-type=servlet"
        );
        if (springContext instanceof WebServerApplicationContext webCtx && webCtx.getWebServer() != null) {
            apiBaseUrl = "http://localhost:" + webCtx.getWebServer().getPort();
        }
    }

    @Override
    public void start(Stage stage) {
        AppState state = new AppState(apiBaseUrl);
        ApiClient api = new ApiClient();

        WorkspacePane workspace = new WorkspacePane(state, api);
        ComposerPane composer = new ComposerPane(state, workspace::fireApiCall);

        HBox topBar = buildTopBar(state);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(composer);
        root.setCenter(workspace);

        state.dirtyProperty().addListener((obs, o, n) -> updateTopBar(topBar, state));

        Scene scene = new Scene(root, 1240, 720);
        String css = getClass().getResource("/gui.css") != null
                ? getClass().getResource("/gui.css").toExternalForm() : null;
        if (css != null) scene.getStylesheets().add(css);

        stage.setTitle("BuildingInfo · API Workbench");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
    }

    private HBox buildTopBar(AppState state) {
        // Brand glyph — three bars + vertical line using styled Labels
        Label bars = new Label("≡");
        bars.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: 700;");
        Label vline = new Label("|");
        vline.setStyle("-fx-font-size: 16px; -fx-text-fill: #3d7da6; -fx-font-weight: 300; -fx-padding: 0 0 0 -4;");
        HBox glyph = new HBox(0, bars, vline);
        glyph.setAlignment(Pos.CENTER);
        glyph.setPadding(new Insets(4, 8, 4, 4));
        glyph.setStyle("-fx-background-color: #2c3446; -fx-background-radius: 4;");

        Label brand = new Label("BuildingInfo");
        brand.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: white;");

        Label subtitle = new Label("· stateless API workbench");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #7c8497;");

        HBox left = new HBox(8, glyph, brand, subtitle);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        // Stats label (updated on dirty)
        Label stats = buildStatsLabel(state);
        stats.setId("stats-label");

        // Server status
        Label dot = new Label("●");
        dot.setStyle("-fx-font-size: 10px; -fx-text-fill: #3ea389;");
        Label serverLabel = new Label(apiBaseUrl.replace("http://", ""));
        serverLabel.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11.5px; -fx-text-fill: #56607a;");

        // Reset button
        Button resetBtn = new Button("Reset");
        resetBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7c8497; -fx-font-size: 12px; -fx-border-color: #3d404a; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
        resetBtn.setOnMouseEntered(e -> resetBtn.setStyle("-fx-background-color: #2c3446; -fx-text-fill: white; -fx-font-size: 12px; -fx-border-color: #56607a; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));
        resetBtn.setOnMouseExited(e -> resetBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7c8497; -fx-font-size: 12px; -fx-border-color: #3d404a; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));
        resetBtn.setOnAction(e -> {
            state.setBuilding(state.makeEmptyBuilding());
            state.setScope(new AppState.Scope.Whole());
            state.setLastResponse("");
            state.markDirty();
        });

        HBox right = new HBox(12, stats, dot, serverLabel, resetBtn);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox bar = new HBox(left, right);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 18, 0, 18));
        bar.setPrefHeight(52);
        bar.setMinHeight(52);
        bar.setMaxHeight(52);
        bar.setStyle("-fx-background-color: #1a1f2e;");
        return bar;
    }

    private void updateTopBar(HBox topBar, AppState state) {
        topBar.getChildren().stream()
                .filter(n -> n instanceof HBox && ((HBox) n).getAlignment() == Pos.CENTER_RIGHT)
                .findFirst()
                .ifPresent(right -> {
                    HBox r = (HBox) right;
                    r.getChildren().stream()
                            .filter(n -> n instanceof Label && "stats-label".equals(n.getId()))
                            .findFirst()
                            .ifPresent(lbl -> ((Label) lbl).setText(buildStatsText(state.getBuilding())));
                });
    }

    private Label buildStatsLabel(AppState state) {
        Label lbl = new Label(buildStatsText(state.getBuilding()));
        lbl.setId("stats-label");
        lbl.setStyle("-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 11.5px; -fx-text-fill: #56607a;");
        return lbl;
    }

    private String buildStatsText(Building b) {
        int levels = b.getLevels().size();
        int rooms = b.getLevels().stream().mapToInt(l -> l.getRooms().size()).sum();
        double area = b.getArea();
        String areaStr = area == Math.floor(area) ? String.valueOf((long) area) : String.format("%.1f", area);
        return levels + " levels · " + rooms + " rooms · " + areaStr + " m²";
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
