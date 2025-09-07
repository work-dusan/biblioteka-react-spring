package pz.bibliotekabackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import pz.bibliotekabackend.model.User;
import pz.bibliotekabackend.repository.UserRepository;
import pz.bibliotekabackend.security.JwtService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository users;
    private final BCryptPasswordEncoder encoder;
    private final JwtService jwt;

    // POST /auth/login  ->  { data: { id, name, email, role, favorites, token } }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq req) {
        String rawEmail = StringUtils.hasText(req.getEmail()) ? req.getEmail() : req.getUsername();
        if (!StringUtils.hasText(rawEmail) || !StringUtils.hasText(req.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }
        String email = rawEmail.trim();
        String password = req.getPassword().trim();

        List<User> list = users.findByEmail(email);
        if (list.isEmpty()) return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));

        User u = list.get(0);
        boolean ok = (u.getPassword() != null && u.getPassword().startsWith("$2"))
                ? encoder.matches(password, u.getPassword())
                : Objects.equals(password, u.getPassword());

        if (!ok) return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));

        String token = jwt.generate(u);
        return ResponseEntity.ok(Map.of("data", flatUser(u, token)));
    }

    // POST /auth/register  ->  { data: { id, name, email, role, favorites, token } }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterReq req) {
        if (!StringUtils.hasText(req.getEmail()) || !StringUtils.hasText(req.getPassword()) || !StringUtils.hasText(req.getName())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name, email and password are required"));
        }
        if (!users.findByEmail(req.getEmail().trim()).isEmpty()) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already exists"));
        }
        User u = new User();
        u.setName(req.getName().trim());
        u.setEmail(req.getEmail().trim());
        u.setRole("user");
        u.setPassword(encoder.encode(req.getPassword().trim()));
        users.save(u);

        String token = jwt.generate(u);
        return ResponseEntity.ok(Map.of("data", flatUser(u, token)));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        String userId = null;

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String p && StringUtils.hasText(p)) {
            userId = p;
        }

        // 2) fallback: parsiraj Authorization header ručno (radi i kada je app.security.enabled=false)
        if (userId == null) {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    var jws = jwt.parse(token);
                    userId = jws.getPayload().getSubject();
                } catch (Exception ignored) {}
            }
        }

        if (!StringUtils.hasText(userId)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        var userOpt = users.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User u = userOpt.get();
        return ResponseEntity.ok(Map.of("data", Map.of(
                "id", u.getId(),
                "name", u.getName(),
                "email", u.getEmail(),
                "role", u.getRole(),
                "favorites", u.getFavorites()
        )));
    }

    private Map<String, Object> flatUser(User u, String token) {
        return Map.of(
                "id", u.getId(),
                "name", u.getName(),
                "email", u.getEmail(),
                "role", u.getRole(),
                "favorites", u.getFavorites(),
                "token", token
        );
    }

    @Data
    public static class LoginReq {
        private String email;
        private String username; // alias
        private String password;
    }

    @Data
    public static class RegisterReq {
        private String name;
        private String email;
        private String password;
    }
}
