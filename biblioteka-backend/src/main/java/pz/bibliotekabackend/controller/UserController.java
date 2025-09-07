package pz.bibliotekabackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pz.bibliotekabackend.model.User;
import pz.bibliotekabackend.security.JwtService;
import pz.bibliotekabackend.service.UserService;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;
    private final JwtService jwt;

    @GetMapping
    public List<User> query(@RequestParam(required = false) String email,
                            @RequestParam(required = false) String password) {
        return users.query(email, password);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable String id) {
        var u = users.get(id);
        return u != null ? ResponseEntity.ok(u) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody User u) {
        var saved = users.create(u); // FE već šalje id (uuidv4) pri registraciji
        return ResponseEntity.created(URI.create("/users/" + saved.getId())).body(saved);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(@PathVariable String id,
                                   @RequestBody Map<String, Object> body,
                                   HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        String callerId = null, callerRole = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                var claims = jwt.parse(auth.substring(7)).getPayload();
                callerId = claims.getSubject();
                callerRole = claims.get("role", String.class);
            } catch (Exception ignored) {}
        }
        boolean isAdmin = callerRole != null && callerRole.equalsIgnoreCase("admin");
        if (!isAdmin && (callerId == null || !callerId.equals(id))) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        var updated = users.patch(id, body);
        return ResponseEntity.ok(java.util.Map.of("data", updated));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        users.delete(id);
        return ResponseEntity.noContent().build();
    }
}
