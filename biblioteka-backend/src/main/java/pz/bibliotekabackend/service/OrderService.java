package pz.bibliotekabackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pz.bibliotekabackend.model.Book;
import pz.bibliotekabackend.model.BookSnapshot;
import pz.bibliotekabackend.model.Order;
import pz.bibliotekabackend.repository.BookRepository;
import pz.bibliotekabackend.repository.OrderRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repo;
    private final BookRepository bookRepo;

    public List<Order> list(String userId, String bookId) {
        List<Order> out;
        if (StringUtils.hasText(userId)) {
            out = repo.findByUserId(userId);
        } else if (StringUtils.hasText(bookId)) {
            out = repo.findByBookId(bookId);
        } else {
            out = repo.findAll();
        }
        out.forEach(this::hydrateDisplayBook);
        return out;
    }

    public Order get(String id) {
        return repo.findById(id)
                .map(o -> {
                    hydrateDisplayBook(o);
                    return o;
                })
                .orElse(null);
    }

    public Order create(Order inbound) {
        if (!StringUtils.hasText(inbound.getUserId())) {
            throw new IllegalArgumentException("Niste prijavljeni (nema userId u tokenu).");
        }

        String bookId = inbound.getBookId();
        if (!StringUtils.hasText(bookId)) {
            var opt = bookRepo.findTopByRentedByOrderByUpdatedAtDesc(inbound.getUserId());
            if (opt.isEmpty()) opt = bookRepo.findTopByRentedByOrderByCreatedAtDesc(inbound.getUserId());
            opt.ifPresent(b -> {
                inbound.setBookId(b.getId());
            });
            bookId = inbound.getBookId();
        }
        if (!StringUtils.hasText(bookId)) {
            throw new IllegalArgumentException("userId i bookId su obavezni");
        }

        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new NoSuchElementException("Knjiga ne postoji"));

        if (book.getRentedBy() == null || !book.getRentedBy().equals(inbound.getUserId())) {
            book.setRentedBy(inbound.getUserId());
            bookRepo.save(book);
        }

        BookSnapshot snap = new BookSnapshot();
        snap.setId(book.getId());
        snap.setTitle(book.getTitle());
        snap.setAuthor(book.getAuthor());
        snap.setYear(book.getYear());
        snap.setImage(book.getImage());

        Instant now = Instant.now();

        Order o = new Order();
        o.setId(null);
        o.setUserId(inbound.getUserId());
        o.setBookId(inbound.getBookId());

        // Upis za oba sveta: i novo (bookSnapshot) i legacy (book)
        o.setBookSnapshot(snap);
        o.setLegacyBook(snap);

        o.setStatus("active");
        o.setRentedAt(now);
        o.setCreatedAt(now);
        o.setUpdatedAt(now);

        Order saved = repo.save(o);
        hydrateDisplayBook(saved);
        return saved;
    }

    public Order patch(String id, Map<String, Object> changes) {
        Order o = repo.findById(id).orElseThrow(NoSuchElementException::new);

        if (changes.containsKey("status")) {
            o.setStatus((String) changes.get("status"));
        }
        if (changes.containsKey("rentedAt")) {
            Object v = changes.get("rentedAt");
            o.setRentedAt(v != null ? Instant.parse(v.toString()) : null);
        }
        if (changes.containsKey("returnedAt")) {
            Object v = changes.get("returnedAt");
            o.setReturnedAt(v != null ? Instant.parse(v.toString()) : null);
        }
        o.setUpdatedAt(Instant.now());

        Order saved = repo.save(o);
        hydrateDisplayBook(saved);
        return saved;
    }

    public Order returnOrder(String id) {
        Order o = repo.findById(id).orElseThrow(NoSuchElementException::new);

        if (o.getReturnedAt() == null) {
            o.setReturnedAt(Instant.now());
            o.setStatus("returned");
            o.setUpdatedAt(Instant.now());
            repo.save(o);
        }

        if (StringUtils.hasText(o.getBookId())) {
            bookRepo.findById(o.getBookId()).ifPresent(b -> {
                b.setRentedBy(null);
                bookRepo.save(b);
            });
        }

        hydrateDisplayBook(o);
        return o;
    }

    public void delete(String id) {
        repo.deleteById(id);
    }

    private void hydrateDisplayBook(Order o) {
        BookSnapshot snap = o.getBookSnapshot();
        if (snap == null) snap = o.getLegacyBook();

        if (snap != null) {
            o.setDisplayBook(Order.DisplayBook.from(snap));
            return;
        }

        if (StringUtils.hasText(o.getBookId())) {
            bookRepo.findById(o.getBookId()).ifPresent(b -> {
                BookSnapshot s = new BookSnapshot();
                s.setId(b.getId());
                s.setTitle(b.getTitle());
                s.setAuthor(b.getAuthor());
                s.setYear(b.getYear());
                s.setImage(b.getImage());
                o.setDisplayBook(Order.DisplayBook.from(s));
            });
        }
    }
}
