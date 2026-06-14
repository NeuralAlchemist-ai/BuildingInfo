package pl.put.poznan.buildinginfo.gui;

import javafx.beans.property.*;
import pl.put.poznan.buildinginfo.model.Building;
import pl.put.poznan.buildinginfo.model.Level;
import pl.put.poznan.buildinginfo.model.Room;

public class AppState {

    public enum Endpoint { AREA, CUBE, LIGHT, HEATING, EXCEEDED }

    public sealed interface Scope permits Scope.Whole, Scope.Lvl, Scope.Rm {
        record Whole() implements Scope {}
        record Lvl(int levelId) implements Scope {}
        record Rm(int roomId, int levelId) implements Scope {}
    }

    private Building building;
    private Scope scope = new Scope.Whole();
    private final ObjectProperty<Endpoint> endpoint = new SimpleObjectProperty<>(Endpoint.AREA);
    private final IntegerProperty threshold = new SimpleIntegerProperty(28);
    private final IntegerProperty dirty = new SimpleIntegerProperty(0);
    private final StringProperty lastResponse = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty offline = new SimpleBooleanProperty(false);
    private String apiBaseUrl = "http://localhost:8080";

    public AppState() {
        this.building = makeEmptyBuilding();
    }

    public AppState(String apiBaseUrl) {
        this();
        this.apiBaseUrl = apiBaseUrl;
    }

    // Creates a minimal building instance used when the GUI starts or resets.
    public Building makeEmptyBuilding() {
        return new Building(1, "Building");
    }

    public void markDirty() {
        dirty.set(dirty.get() + 1);
    }

    public Building getBuilding() { return building; }
    public void setBuilding(Building b) { this.building = b; }

    public Scope getScope() { return scope; }
    public void setScope(Scope s) { this.scope = s; }

    public ObjectProperty<Endpoint> endpointProperty() { return endpoint; }
    public Endpoint getEndpoint() { return endpoint.get(); }
    public void setEndpoint(Endpoint e) { endpoint.set(e); }

    public IntegerProperty thresholdProperty() { return threshold; }
    public int getThreshold() { return threshold.get(); }

    public IntegerProperty dirtyProperty() { return dirty; }

    public StringProperty lastResponseProperty() { return lastResponse; }
    public String getLastResponse() { return lastResponse.get(); }
    public void setLastResponse(String s) { lastResponse.set(s); }

    public BooleanProperty loadingProperty() { return loading; }
    public boolean isLoading() { return loading.get(); }
    public void setLoading(boolean b) { loading.set(b); }

    public BooleanProperty offlineProperty() { return offline; }
    public boolean isOffline() { return offline.get(); }
    public void setOffline(boolean b) { offline.set(b); }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    // Builds a small sample tree with levels and rooms for quick demo data.
    public Building makeSeedBuilding() {
        Building b = new Building(1, "Block C");

        Level ground = new Level(10, "Ground Floor");
        ground.addRoom(new Room(101, "Reception",    28.5, 85.5,  980,  540));
        ground.addRoom(new Room(102, "Conference A", 42.0, 126.0, 2520, 880));

        Level floor1 = new Level(20, "Floor 1");
        floor1.addRoom(new Room(201, "Open Office",    96.0, 288.0, 4320, 2150));
        floor1.addRoom(new Room(202, "Server Closet",   8.0,  24.0, 1680,   60));

        b.addLevel(ground);
        b.addLevel(floor1);
        return b;
    }
}
