package pz.bibliotekabackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pz.bibliotekabackend.model.Order;
import pz.bibliotekabackend.security.JwtService;
import pz.bibliotekabackend.service.OrderService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orders;
    private final JwtService jwt;
    /**
     * GET /orders
     * - Admin: vidi sve (ili filtrira ?userId=...), opciono ?bookId=...
     * - User: uvek vidi SAMO svoje (ignoriše bilo koji userId u query-ju)
     */
    @GetMapping
    public ResponseEntity<List<Order>> list(@RequestParam(required = false) String userId,
                                            @RequestParam(required = false) String bookId,
                                            HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        String callerId = null;
        String callerRole = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                var claims = jwt.parse(auth.substring(7)).getPayload();
                callerId = claims.getSubject();
                callerRole = claims.get("role", String.class);
            } catch (Exception ignored) {}
        }
        boolean isAdmin = callerRole != null && callerRole.equalsIgnoreCase("admin");
        String effectiveUserId = isAdmin ? userId : (callerId != null ? callerId : null);
        if (!isAdmin && effectiveUserId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(orders.list(effectiveUserId, bookId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> get(@PathVariable String id) {
        var o = orders.get(id);
        return o != null ? ResponseEntity.ok(o) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        // 1) Izvuci userId iz JWT-a
        String auth = request.getHeader("Authorization");
        String callerId = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                var claims = jwt.parse(auth.substring(7)).getPayload();
                callerId = claims.getSubject();
            } catch (Exception ignored) {}
        }

        // 2) Uhvati bookId iz raznih mogućih ključeva u telu
        String bookId = null;
        Object v;
        if ((v = body.get("bookId")) instanceof String s1) bookId = s1;
        else if ((v = body.get("book_id")) instanceof String s2) bookId = s2;
        else if ((v = body.get("book")) instanceof String s3) bookId = s3;
        else if ((v = body.get("idBook")) instanceof String s4) bookId = s4;
        else if ((v = body.get("book")) instanceof Map<?,?> m) {
            Object id = m.get("id");
            if (id != null) bookId = id.toString();
        }
        // (fallback kroz query param)
        if (bookId == null) {
            String qp = request.getParameter("bookId");
            if (qp != null && !qp.isBlank()) bookId = qp;
        }

        Order inbound = new Order();
        inbound.setUserId(callerId);
        inbound.setBookId(bookId);

        Order saved = orders.create(inbound);
        return ResponseEntity.created(URI.create("/orders/" + saved.getId())).body(saved);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Order> patch(@PathVariable String id, @RequestBody Map<String,Object> body) {
        return ResponseEntity.ok(orders.patch(id, body));
    }

    @PatchMapping("/{id}/return")
    public ResponseEntity<?> returnOrder(@PathVariable String id) {
        try {
            Order updated = orders.returnOrder(id);
            return ResponseEntity.ok(Map.of("data", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        orders.delete(id);
        return ResponseEntity.noContent().build();
    }
}
