package pz.bibliotekabackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pz.bibliotekabackend.model.Book;
import pz.bibliotekabackend.model.User;
import pz.bibliotekabackend.repository.BookRepository;
import pz.bibliotekabackend.repository.OrderRepository;
import pz.bibliotekabackend.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repo;
    private final BookRepository bookRepo;
    private final OrderRepository orderRepo;
    private final BCryptPasswordEncoder encoder;

    public List<User> query(String email, String password) {
        if (StringUtils.hasText(email) && StringUtils.hasText(password)) {
            var users = repo.findByEmail(email);
            return users.stream()
                    .filter(u -> {
                        var stored = u.getPassword();
                        boolean hashed = stored != null && stored.startsWith("$2");
                        return hashed
                                ? encoder.matches(password, stored)
                                : Objects.equals(password, stored);
                    })
                    .toList();
        }
        if (StringUtils.hasText(email)) {
            return repo.findByEmail(email);
        }
        return repo.findAll();
    }

    public User create(User u) {
        if (StringUtils.hasText(u.getPassword()) && !u.getPassword().startsWith("$2")) {
            u.setPassword(encoder.encode(u.getPassword()));
        }
        return repo.save(u);
    }

    public User patch(String id, Map<String, Object> changes) {
        var u = repo.findById(id).orElseThrow();
        if (changes.containsKey("password")) {
            var raw = (String) changes.get("password");
            if (StringUtils.hasText(raw) && !raw.startsWith("$2")) {
                u.setPassword(encoder.encode(raw));
            } else {
                u.setPassword(raw);
            }
        }
        if (changes.containsKey("name")) u.setName((String) changes.get("name"));
        if (changes.containsKey("email")) u.setEmail((String) changes.get("email"));
        if (changes.containsKey("role")) u.setRole((String) changes.get("role"));
        if (changes.containsKey("favorites")) {
            @SuppressWarnings("unchecked")
            var favs = (List<Object>) changes.get("favorites");
            u.setFavorites(favs.stream().map(Object::toString).toList());
        }
        return repo.save(u);
    }

    public void delete(String id) {
        orderRepo.deleteByUserId(id);

        List<Book> rented = bookRepo.findByRentedBy(id);
        rented.forEach(b -> b.setRentedBy(null));
        if (!rented.isEmpty()) {
            bookRepo.saveAll(rented);
        }

        repo.deleteById(id);
    }
    public User get(String id) { return repo.findById(id).orElse(null); }
}
