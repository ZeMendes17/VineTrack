package pt.ua.ies.vineTrack.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.ua.ies.vineTrack.entity.Grape;
import pt.ua.ies.vineTrack.entity.Notification;
import pt.ua.ies.vineTrack.entity.Track;
import pt.ua.ies.vineTrack.entity.User;
import pt.ua.ies.vineTrack.entity.Vine;
import pt.ua.ies.vineTrack.service.GrapeService;
import pt.ua.ies.vineTrack.service.NotificationService;
import pt.ua.ies.vineTrack.service.TrackService;
import pt.ua.ies.vineTrack.service.UserService;
import pt.ua.ies.vineTrack.service.VineService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;


@CrossOrigin("*")
@RestController
@RequestMapping(path = "/vines")
public class VineController {
    @Autowired
    private VineService vineService;
    @Autowired
    private UserService userService;
    @Autowired
    private GrapeService grapeService;
    @Autowired
    private TrackService trackService;
    @Autowired
    private NotificationService notificationService;

    private static final double MAX_WATER_CONSUMPTION = 0.95; // max of 0.95L per m^2 per day

    @GetMapping(path = "/test")
    public Track getAllVinesTest(){
        // Returns the last track
        List<Track> tracks = vineService.getTracksByVineId(1);
        return tracks.get(tracks.size() - 1);
    }

    @GetMapping(path = "/moisture/{vineId}")
    public List<Double> getMoistureByVineId(@PathVariable int vineId){
        List<Track> tracks = vineService.getTracksByVineId(vineId);
        Iterator<Track> iterator = tracks.iterator();
        while (iterator.hasNext()) {
            Track track = iterator.next();
            if (!track.getType().equals("moisture")) {
                iterator.remove();
            }
        }
        // now we need to order the tracks by date from the oldest to the newest
        tracks.sort(Comparator.comparing(Track::getDate));

        // finally we need to get only the moisture values
        List<Double> moistureValues = new ArrayList<>(tracks.stream().map(Track::getValue).toList());
        while (moistureValues.size() < 10) {
            moistureValues.add(0, 0.0);
        }
        if (moistureValues.size() > 10) {
            moistureValues = moistureValues.subList(moistureValues.size() - 10, moistureValues.size());
        }
        System.out.println("Vine: " + vineId + " - " + "Moisture: " + moistureValues);
        return moistureValues;
    }

    @GetMapping(path = "/temperature/{vineId}")
    public Map<String, Double> getTemperatureByVineId(@PathVariable int vineId){
        List<Track> tracks = vineService.getTracksByVineId(vineId);
        Iterator<Track> iterator = tracks.iterator();
        while (iterator.hasNext()) {
            Track track = iterator.next();
            if (!track.getType().equals("temperature")) {
                iterator.remove();
            }
        }

        // now we need to order the tracks by date from the oldest to the newest
        tracks.sort(Comparator.comparing(Track::getDate));

        // finally we need to get only the moisture values

        List<Double> tempValues = new ArrayList<>();
        List<String> tempTimes = new ArrayList<>();

        for (Track track : tracks) {
            if (track.getDay().equals(LocalDate.now().toString())) {
                tempValues.add(track.getValue());
                tempTimes.add(track.getTime());
            }
        }

        Map<String, Double> tempMap = new TreeMap<>();
        for (int i = 0; i < tempValues.size(); i++) {
            tempMap.put(tempTimes.get(i), tempValues.get(i));
        }

        System.out.println("Vine: " + vineId + " - " + "Temperature: " + tempMap);
        return tempMap;
    }

    @GetMapping(path = "/weatherAlerts/{vineId}")
    public Map<String, List<String>> getWeatherAlertsByVineId(@PathVariable int vineId) throws JsonMappingException, JsonProcessingException{
        List<Track> tracks = vineService.getTracksByVineId(vineId);
        Iterator<Track> iterator = tracks.iterator();
        while (iterator.hasNext()) {
            Track track = iterator.next();
            if (!track.getType().equals("weatherAlerts")) {
                iterator.remove();
            }
        }

        // now we need to order the tracks by date from the oldest to the newest
        tracks.sort(Comparator.comparing(Track::getDate));

        // finally we need to get only the moisture values

        Map<String, List<String>> map = new TreeMap<>();

        for (Track track : tracks) {
            String weatherAlerts = track.getValString();
            weatherAlerts = weatherAlerts.replace("'", "\"");

            ObjectMapper mapper = new ObjectMapper();
            map = mapper.readValue(weatherAlerts, new TypeReference<Map<String, List<String>>>() {});
        }

        System.out.println("Vine: " + vineId + " - " + "Weather: " + map);
        return map;
    }

    @GetMapping(path = "/waterConsumption/{vineId}")
    public List<Double> getWaterConsumptionByVineId(@PathVariable int vineId){
        List<Track> tracks = vineService.getTracksByVineId(vineId);

        tracks.removeIf(track -> !track.getType().equals("waterConsumption"));

        // map to store day: waterConsumption for that day
        Map<String, Double> waterConsumptionMap = new TreeMap<>();

        for (Track track : tracks) {
            String day = track.getDay();
            double waterConsumption = track.getValue();
            if (waterConsumptionMap.containsKey(day)) {
                waterConsumptionMap.put(day, waterConsumptionMap.get(day) + waterConsumption);
            } else {
                waterConsumptionMap.put(day, waterConsumption);
            }
        }

        System.out.println("Vine: " + vineId + " - " + "Water consumption: " + waterConsumptionMap);

        List<Double> waterConsumptionValues = new ArrayList<>(waterConsumptionMap.values());
        while (waterConsumptionValues.size() < 8) {
            waterConsumptionValues.add(0, 0.0);
        }

        if (waterConsumptionValues.size() > 8) {
            waterConsumptionValues = waterConsumptionValues.subList(waterConsumptionValues.size() - 8, waterConsumptionValues.size());
        }

        return waterConsumptionValues;
    }

    @GetMapping(path = "/name/{vineId}")
    public String getVineNameById(@PathVariable Integer vineId){
        try {
            return vineService.getVineById(vineId).getName();
        } catch (Exception e) {
            return "Vine not found";
        }
    }


    @GetMapping()
    public ResponseEntity<List<Vine>> getAllVines(){
        try {
            return ResponseEntity.ok(vineService.getAllVines());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }

    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Vine> addVine(@RequestParam String name, @RequestParam String location, @RequestParam String city, @RequestParam Double size, @RequestParam java.util.Date date, @RequestParam(required = false) MultipartFile img, @RequestParam List<Integer> users, @RequestParam List<Integer> typeGrap){

        try {
            Vine vine = new Vine();
            vine.setName(name);
            vine.setLocation(location);
            vine.setCity(city);
            vine.setSize(size);
            vine.setImage("");
            vine.setDate(new Date(date.getTime()));
            vine.setMaxWaterConsumption(MAX_WATER_CONSUMPTION*vine.getSize()); // default value defined
            for (Integer id : users) {
                User user = userService.getUserById(id);
                if (user.getVines() != null) {
                    if (!user.getVines().contains(vine)) {
                        user.getVines().add(vine);
                    }
                } else {
                    List<Vine> vines = new ArrayList<Vine>();
                    vines.add(vine);
                    user.setVines(vines);
                }
            }

            for (Integer id : typeGrap){
                Grape grape = grapeService.getGrapeById(id);
                if (vine.getTypeGrap() != null) {
                    if (!vine.getTypeGrap().contains(grape)) {
                        vine.getTypeGrap().add(grape);
                    }
                } else {
                    List<Grape> grapes = new ArrayList<Grape>();
                    grapes.add(grape);
                    vine.setTypeGrap(grapes);
                }
            }

            vineService.save(vine);

            // add 2 tracks to the vine
            LocalDateTime now = LocalDateTime.now();
            Track track2 = new Track("moisture", now, 0.0, vine, now.toLocalTime().toString(), now.toLocalDate().toString());
            Track track1 = new Track("moisture", LocalDateTime.now(), 0.0, vine, now.toLocalTime().toString(), now.toLocalDate().toString());
            trackService.saveTrack(track1);
            trackService.saveTrack(track2);

            if (!img.isEmpty()){
                try {
                    byte[] bytes = img.getBytes();
                    Path path = Paths.get("src/main/resources/static/vines/" + vine.getId() + "_" + vine.getName().replaceAll("\s", "") + ".jpeg");
                    Files.write(path, bytes);
                    vine.setImage(path.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return ResponseEntity.ok(vineService.save(vine));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<Vine> getVineById(@PathVariable Integer id){
        try {
            return ResponseEntity.ok(vineService.getVineById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/image/{id}")
    public ResponseEntity<byte[]> getImageById(@PathVariable Integer id) throws IOException{
        Vine vine = vineService.getVineById(id);
        Path path = Paths.get(vine.getImage());
        byte[] image = Files.readAllBytes(path);

        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<String> deleteVineById(@PathVariable Integer id){
        try {
            for (User user : vineService.getVineById(id).getUsers()) {
                user.getVines().remove(vineService.getVineById(id));
            }

            for (Track track : trackService.getAllTracks()){
                if (track.getVine().getId() == id){
                    trackService.deleteTrackById(track.getId());
                }
            }

            for (Notification noti : notificationService.getNotificationsByVine(vineService.getVineById(id))){
                notificationService.deleteNotificationById(noti.getId());
            }

            return ResponseEntity.ok(vineService.deleteVineById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/waterLimit/{vineId}")
    public ResponseEntity<Vine> UpdateWaterLimit(@PathVariable int vineId, @RequestBody Map<String, Double> requestBody) {
        try {
            Double waterLimit = requestBody.get("waterLimit");
            System.out.println("Received water limit: " + waterLimit + " for vine: " + vineId);
            Vine vine = vineService.getVineById(vineId);
            vine.setMaxWaterConsumption(waterLimit);
            System.out.println("New water limit: " + vine.getMaxWaterConsumption());
            vineService.save(vine);
            return ResponseEntity.ok(vine);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/waterLimit/{vineId}")
    public ResponseEntity<Double> getWaterLimit(@PathVariable int vineId) {
        try {
            Double waterLimit = vineService.getVineById(vineId).getMaxWaterConsumption();
            System.out.println("Received water limit: " + waterLimit + " for vine: " + vineId);
            return ResponseEntity.ok(waterLimit);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping(path = "/ph/{vineId}")
    public ResponseEntity<Map<String, String>> addPhTrack(@PathVariable Integer vineId, @RequestParam Double value){
        try {
            System.out.println("Received ph value: " + value);
            Vine vine = vineService.getVineById(vineId);
            LocalDateTime now = LocalDateTime.now();
            Track track = new Track("ph", now, value, vine, now.toLocalTime().toString(), now.toLocalDate().toString());
            trackService.saveTrack(track);

            // only keep the last 5 tracks for each vine
            List<Track> tracks = trackService.getTracksByVineId(vineId);
            tracks.removeIf(track1 -> !track1.getType().equals("ph"));
            // order the tracks by date from the oldest to the newest
            tracks.sort(Comparator.comparing(Track::getDate));
            while (tracks.size() > 5) {
                trackService.deleteTrackById(tracks.get(0).getId());
                tracks.remove(0);
            }

            Map<String, String> map = new HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
            LocalDateTime date = LocalDateTime.parse(now.toString(), formatter);
            // Define the desired output format
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

            // Format the LocalDateTime object into a readable format
            String formattedDateTime = date.format(outputFormatter);
            map.put("date", formattedDateTime);
            map.put("value", value.toString());

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/ph/{vineId}") // gets the date and value of the ph tracks
    public ResponseEntity<List<Map<String, String>>> getPhTracks(@PathVariable Integer vineId){
        try {
            List<Track> tracks = trackService.getTracksByVineId(vineId);
            tracks.removeIf(track -> !track.getType().equals("ph"));

            // now we need to order the tracks by date from the newest to the oldest
            tracks.sort(Comparator.comparing(Track::getDate).reversed());

            List<Map<String, String>> list = new ArrayList<>();

            for (Track track : tracks) {
                Map<String, String> map = new HashMap<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
                LocalDateTime date = LocalDateTime.parse(track.getDay() + "T" + track.getTime(), formatter);
                // Define the desired output format
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

                // Format the LocalDateTime object into a readable format
                String formattedDateTime = date.format(outputFormatter);
                map.put("date", formattedDateTime);
                map.put("value", Double.toString(track.getValue()));
                list.add(map);
            }

            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
