package pt.ua.ies.vineTrack.message_broker;

import org.json.JSONObject;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import pt.ua.ies.vineTrack.entity.Track;
import pt.ua.ies.vineTrack.entity.Vine;
import pt.ua.ies.vineTrack.entity.Notification;
import pt.ua.ies.vineTrack.service.TrackService;
import pt.ua.ies.vineTrack.service.VineService;
import pt.ua.ies.vineTrack.service.NotificationService;


import java.time.LocalDateTime;

@Component
public class RabbitmqHandler {
    @Autowired
    private SimpMessagingTemplate template; // for sending messages to the client through websocket
    @Autowired
    private VineService vineService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TrackService trackService;

    private static final int MAX_NOTIFICATIONS = 10;
    @RabbitListener(queues = Config.QUEUE_NAME)
    public void receiveMessage(String message) {
        System.out.println("Received <" + message + ">");
        this.template.convertAndSend("/topic/update", message);
        JSONObject params = new JSONObject(message);
        String type = params.getString("sensor");

        switch (type) {
            case "moisture":
                int vineId = params.getInt("id");
                double value = params.getDouble("value");

                // store the track in the database
                Vine vine = vineService.getVineById(vineId);
                LocalDateTime date = LocalDateTime.now();
                Track track = new Track(type, date, value, vine);

                trackService.saveTrack(track);
                // only have 10 tracks per vine, remove the oldest
                trackService.removeOldTracks("moisture");

                // receive message, if the value is bellow expected save notification to the database
                // for now we will consider that the expected value is 40
                if (value < 40) {
                    Boolean isUnRead = true;
                    Notification notification = new Notification("moisture", "/public/assets/images/notifications/water.png", isUnRead, vine);
                    // set description to  'Levels of the soil humidity are low.'
                    notification.setDescription("Levels of the soil humidity are low.");

                    int totalNotifications = notificationService.getNumberOfNotificationsByVine(vine);

                    // If the total exceeds the maximum limit, remove older notifications
                    if (totalNotifications > MAX_NOTIFICATIONS) {
                        notificationService.removeOldestNotificationsForVine(vine.getId(), MAX_NOTIFICATIONS);
                    }


                    // send through websocket
                    JSONObject notificationJson = new JSONObject(notification);
                    this.template.convertAndSend("/topic/notification", notificationJson.toString());

                    notificationService.saveNotification(notification);
                }

                break;
            case "temperature":
                int vineId2 = params.getInt("id");
                double value2 = params.getDouble("value");
                String time = params.getString("date");
                String day = params.getString("day");

                Vine vine2 = vineService.getVineById(vineId2);
                LocalDateTime date2 = LocalDateTime.now();

                Track track2 = new Track(type, date2, value2, vine2, time, day);

                trackService.saveTrack(track2);
                trackService.removeOldTracks("temperature");
                
                vine2.setTemperature(value2);
                vineService.save(vine2);

                break;
            default:
                break;
        }
    }
}
