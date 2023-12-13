package pt.ua.ies.vineTrack.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.ua.ies.vineTrack.service.UserService;
import pt.ua.ies.vineTrack.entity.User;
import pt.ua.ies.vineTrack.entity.Notification;
import pt.ua.ies.vineTrack.service.NotificationService;
import pt.ua.ies.vineTrack.entity.Vine;

import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping(path = "/api/users")
@ResponseStatus(HttpStatus.OK)
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping()
    public ResponseEntity<List<User>> getAllUsers(){
        try {
                        return ResponseEntity.ok(userService.getAllUsers());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping()
    public ResponseEntity<User> addUser(@Valid @RequestBody User user){
                return ResponseEntity.ok(userService.save(user));
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<User> viewUser(@PathVariable Integer id){
        try {
            return ResponseEntity.ok(userService.getUserById(id));
                    } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    

    @GetMapping(path = "/email/{email}")
    public ResponseEntity<User> viewUser(@PathVariable String email){
        try {
            return ResponseEntity.ok(userService.getUserByEmail(email));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/vines/{userId}")
    public ResponseEntity<List<Integer>> getVinesByUserId(@PathVariable Integer userId){
        try {
            return ResponseEntity.ok(userService.getVinesIds(userId));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/notifications/{userId}")
    // get all notifications from a vine
    public List<Notification> getNotificationsByUserId(@PathVariable Integer userId){
        User user = userService.getUserById(userId);
        List<Vine> vines = userService.getVinesByUser(user);
        List<Notification> notifications = new ArrayList<>();
        for (Vine vine : vines) {
                notifications.addAll(notificationService.getNotificationsByVineId(vine.getId()));
        }
        // invert the list
        int count = 1;
        // order notifications by date from newer to older
        notifications.sort(Comparator.comparing(Notification::getDate).reversed());

        List<Notification> notificationsToRemove = new ArrayList<>();
        Map<Integer, String> map = new HashMap<>();
        for (Notification notification : notifications) {
            if (map.containsKey(notification.getVineId()) && map.get(notification.getVineId()).equals(notification.getType())) {
                notificationsToRemove.add(notification);
            } else {
                map.put(notification.getVineId(), notification.getType());
            }
        }
        // remove duplicated notifications
        notifications.removeAll(notificationsToRemove);
        notificationService.deleteNotifications(notificationsToRemove);
        List<Notification> invertedNotifications = new ArrayList<>();
        // order notifications by date from older to newer
        notifications.sort(Comparator.comparing(Notification::getDate));
        for (int i = notifications.size() - 1; i >= 0; i--) {
            count++;
            invertedNotifications.add(notifications.get(i));
            if (count == 11) {
                break;
            }
        }
        for (Notification notification : invertedNotifications) {
            System.out.println(notification.getType() + " " + notification.getDate());
        }
        return invertedNotifications;
    }


    @PutMapping("/markAsRead/{notificationId}")
    public ResponseEntity<Notification> markNotificationAsRead(@PathVariable int notificationId) {
        try {
            Notification notification = notificationService.getNotificationById(notificationId);

            if (notification != null && !notification.isRead()) {
                notification.setIsUnRead(false);  // Update isUnRead to false
                notificationService.saveNotification(notification);
                return ResponseEntity.ok(notification);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @PutMapping()
    public User updateUser(@RequestBody User user){
        return userService.updateUser(user);
    }

    @DeleteMapping(path = "/{id}")
    public String deleteUser(@PathVariable Integer id){
        return userService.deleteUserById(id);
    }
}
