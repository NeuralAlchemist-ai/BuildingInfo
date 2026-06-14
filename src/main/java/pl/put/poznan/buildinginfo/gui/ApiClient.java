package pl.put.poznan.buildinginfo.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    // Sends the current building JSON to the selected endpoint and updates the UI.
    public void fireAndUpdate(AppState state, Runnable onDone) {
        state.setLoading(true);
        state.markDirty();

        String body;
        try {
            body = mapper.writeValueAsString(state.getBuilding());
        } catch (Exception e) {
            Platform.runLater(() -> {
                state.setLastResponse("// JSON serialization error: " + e.getMessage());
                state.setLoading(false);
                state.setOffline(false);
                state.markDirty();
                onDone.run();
            });
            return;
        }

        String url = buildUrl(state);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(8))
                .build();

        http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .whenComplete((resp, err) -> Platform.runLater(() -> {
                    if (err != null || resp == null) {
                        state.setOffline(true);
                        state.setLastResponse("// Could not reach " + state.getApiBaseUrl());
                    } else {
                        state.setOffline(false);
                        state.setLastResponse(prettyPrint(resp.body()));
                    }
                    state.setLoading(false);
                    state.markDirty();
                    onDone.run();
                }));
    }

    private String buildUrl(AppState state) {
        String base = state.getApiBaseUrl();
        AppState.Endpoint ep = state.getEndpoint();
        AppState.Scope scope = state.getScope();

        if (ep == AppState.Endpoint.EXCEEDED) {
            return base + "/api/building/heating/exceeded?threshold=" + state.getThreshold();
        }

        String metric = switch (ep) {
            case AREA    -> "area";
            case CUBE    -> "cube";
            case LIGHT   -> "light";
            case HEATING -> "heating";
            default      -> "area";
        };

        return switch (scope) {
            case AppState.Scope.Whole w  -> base + "/api/building/" + metric;
            case AppState.Scope.Lvl lvl  -> base + "/api/building/level/" + lvl.levelId() + "/" + metric;
            case AppState.Scope.Rm rm    -> base + "/api/building/room/" + rm.roomId() + "/" + metric;
        };
    }

    private String prettyPrint(String json) {
        try {
            Object obj = mapper.readValue(json, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return json;
        }
    }
}
