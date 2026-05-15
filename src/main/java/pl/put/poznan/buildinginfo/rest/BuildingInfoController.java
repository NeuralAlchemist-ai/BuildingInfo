package pl.put.poznan.buildinginfo.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.put.poznan.buildinginfo.logic.BuildingInfoTransformer;
import pl.put.poznan.buildinginfo.model.Building;
import pl.put.poznan.buildinginfo.model.Level;
import pl.put.poznan.buildinginfo.model.Room;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller exposing the Building Info API.
 *
 * <p>All endpoints accept a {@link Building} as a JSON request body and return
 * computed statistics in JSON format. The building structure mirrors the data
 * model described in the project specification:</p>
 * <pre>
 * {
 *   "id": 1, "name": "Main Building",
 *   "levels": [
 *     { "id": 10, "name": "Ground Floor",
 *       "rooms": [
 *         { "id": 101, "name": "Room A", "area": 30.0, "cube": 75.0,
 *           "heating": 1200.0, "light": 500.0 }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p>Base path: {@code /api/building}</p>
 */
@RestController
@RequestMapping("/api/building")
public class BuildingInfoController {

    private static final Logger logger = LoggerFactory.getLogger(BuildingInfoController.class);

    private final BuildingInfoTransformer transformer = new BuildingInfoTransformer();

    // ── Building-level endpoints ───────────────────────────────

    /**
     * Returns the total floor area of the building in m².
     *
     * @param building the building structure in JSON
     * @return JSON: {@code {"area": <value>}}
     */
    @PostMapping("/area")
    public ResponseEntity<Map<String, Double>> getBuildingArea(@RequestBody Building building) {
        logger.debug("POST /api/building/area – building id={}", building.getId());
        double area = transformer.getArea(building);
        logger.info("Building {} area = {} m²", building.getId(), area);
        return ResponseEntity.ok(Map.of("area", area));
    }

    /**
     * Returns the total volume (cubature) of the building in m³.
     *
     * @param building the building structure in JSON
     * @return JSON: {@code {"cube": <value>}}
     */
    @PostMapping("/cube")
    public ResponseEntity<Map<String, Double>> getBuildingCube(@RequestBody Building building) {
        logger.debug("POST /api/building/cube – building id={}", building.getId());
        double cube = transformer.getCube(building);
        logger.info("Building {} cube = {} m³", building.getId(), cube);
        return ResponseEntity.ok(Map.of("cube", cube));
    }

    /**
     * Returns the average lighting power per m² for the building.
     *
     * @param building the building structure in JSON
     * @return JSON: {@code {"lightPerArea": <value>}}
     */
    @PostMapping("/light")
    public ResponseEntity<Map<String, Double>> getBuildingLightPerArea(@RequestBody Building building) {
        logger.debug("POST /api/building/light – building id={}", building.getId());
        double light = transformer.getLightPerArea(building);
        return ResponseEntity.ok(Map.of("lightPerArea", light));
    }

    /**
     * Returns the average heating energy consumption per m³ for the building.
     *
     * @param building the building structure in JSON
     * @return JSON: {@code {"heatingPerCube": <value>}}
     */
    @PostMapping("/heating")
    public ResponseEntity<Map<String, Double>> getBuildingHeatingPerCube(@RequestBody Building building) {
        logger.debug("POST /api/building/heating – building id={}", building.getId());
        double heating = transformer.getHeatingPerCube(building);
        return ResponseEntity.ok(Map.of("heatingPerCube", heating));
    }

    /**
     * Returns all rooms whose heating per m³ exceeds the given threshold.
     *
     * @param threshold the limit value for heating per m³
     * @param building  the building structure in JSON
     * @return JSON array of room objects exceeding the threshold
     */
    @PostMapping("/heating/exceeded")
    public ResponseEntity<List<Room>> getRoomsExceedingHeating(
            @RequestParam double threshold,
            @RequestBody Building building) {
        logger.debug("POST /api/building/heating/exceeded – threshold={}, building id={}",
                threshold, building.getId());
        List<Room> rooms = transformer.getRoomsExceedingHeatingThreshold(building, threshold);
        logger.info("Found {} rooms exceeding heating threshold {}", rooms.size(), threshold);
        return ResponseEntity.ok(rooms);
    }

    // ── Level-level endpoints ──────────────────────────────────

    /**
     * Returns the total floor area of a specific level in m².
     *
     * @param levelId  the id of the level to query
     * @param building the building structure in JSON
     * @return JSON: {@code {"area": <value>}} or 404 if level not found
     */
    @PostMapping("/level/{levelId}/area")
    public ResponseEntity<Map<String, Double>> getLevelArea(
            @PathVariable int levelId,
            @RequestBody Building building) {
        logger.debug("POST /api/building/level/{}/area", levelId);
        Optional<Level> level = transformer.findLevel(building, levelId);
        if (level.isEmpty()) {
            logger.warn("Level {} not found in building {}", levelId, building.getId());
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("area", transformer.getArea(level.get())));
    }

    /**
     * Returns the total volume of a specific level in m³.
     *
     * @param levelId  the id of the level to query
     * @param building the building structure in JSON
     * @return JSON: {@code {"cube": <value>}} or 404 if level not found
     */
    @PostMapping("/level/{levelId}/cube")
    public ResponseEntity<Map<String, Double>> getLevelCube(
            @PathVariable int levelId,
            @RequestBody Building building) {
        logger.debug("POST /api/building/level/{}/cube", levelId);
        Optional<Level> level = transformer.findLevel(building, levelId);
        if (level.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("cube", transformer.getCube(level.get())));
    }

    /**
     * Returns the average lighting power per m² for a specific level.
     *
     * @param levelId  the id of the level to query
     * @param building the building structure in JSON
     * @return JSON: {@code {"lightPerArea": <value>}} or 404 if level not found
     */
    @PostMapping("/level/{levelId}/light")
    public ResponseEntity<Map<String, Double>> getLevelLightPerArea(
            @PathVariable int levelId,
            @RequestBody Building building) {
        logger.debug("POST /api/building/level/{}/light", levelId);
        Optional<Level> level = transformer.findLevel(building, levelId);
        if (level.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("lightPerArea", transformer.getLightPerArea(level.get())));
    }

    /**
     * Returns the average heating per m³ for a specific level.
     *
     * @param levelId  the id of the level to query
     * @param building the building structure in JSON
     * @return JSON: {@code {"heatingPerCube": <value>}} or 404 if level not found
     */
    @PostMapping("/level/{levelId}/heating")
    public ResponseEntity<Map<String, Double>> getLevelHeatingPerCube(
            @PathVariable int levelId,
            @RequestBody Building building) {
        logger.debug("POST /api/building/level/{}/heating", levelId);
        Optional<Level> level = transformer.findLevel(building, levelId);
        if (level.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("heatingPerCube", transformer.getHeatingPerCube(level.get())));
    }

    // ── Room-level endpoints ───────────────────────────────────

    /**
     * Returns the floor area of a specific room in m².
     *
     * @param roomId   the id of the room to query
     * @param building the building structure in JSON
     * @return JSON: {@code {"area": <value>}} or 404 if room not found
     */
    @PostMapping("/room/{roomId}/area")
    public ResponseEntity<Map<String, Double>> getRoomArea(
            @PathVariable int roomId,
            @RequestBody Building building) {
        logger.debug("POST /api/building/room/{}/area", roomId);
        Optional<Room> room = transformer.findRoom(building, roomId);
        if (room.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("area", transformer.getArea(room.get())));
    }

    /**
     * Returns the volume of a specific room in m³.
     *
     * @param roomId   the id of the room to query
     * @param building the building structure in JSON
     * @return JSON: {@code {"cube": <value>}} or 404 if room not found
     */
    @PostMapping("/room/{roomId}/cube")
    public ResponseEntity<Map<String, Double>> getRoomCube(
            @PathVariable int roomId,
            @RequestBody Building building) {
        logger.debug("POST /api/building/room/{}/cube", roomId);
        Optional<Room> room = transformer.findRoom(building, roomId);
        if (room.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("cube", transformer.getCube(room.get())));
    }

    /**
     * Returns the lighting power per m² for a specific room.
     *
     * @param roomId   the id of the room to query
     * @param building the building structure in JSON
     * @return JSON: {@code {"lightPerArea": <value>}} or 404 if room not found
     */
    @PostMapping("/room/{roomId}/light")
    public ResponseEntity<Map<String, Double>> getRoomLightPerArea(
            @PathVariable int roomId,
            @RequestBody Building building) {
        logger.debug("POST /api/building/room/{}/light", roomId);
        Optional<Room> room = transformer.findRoom(building, roomId);
        if (room.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("lightPerArea", transformer.getLightPerArea(room.get())));
    }

    /**
     * Returns the heating per m³ for a specific room.
     *
     * @param roomId   the id of the room to query
     * @param building the building structure in JSON
     * @return JSON: {@code {"heatingPerCube": <value>}} or 404 if room not found
     */
    @PostMapping("/room/{roomId}/heating")
    public ResponseEntity<Map<String, Double>> getRoomHeatingPerCube(
            @PathVariable int roomId,
            @RequestBody Building building) {
        logger.debug("POST /api/building/room/{}/heating", roomId);
        Optional<Room> room = transformer.findRoom(building, roomId);
        if (room.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("heatingPerCube", transformer.getHeatingPerCube(room.get())));
    }
}
