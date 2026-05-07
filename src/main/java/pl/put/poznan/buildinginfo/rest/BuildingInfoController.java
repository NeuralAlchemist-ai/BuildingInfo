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

}