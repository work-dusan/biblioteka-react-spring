package pz.bibliotekabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pz.bibliotekabackend.model.Book;
import pz.bibliotekabackend.repository.BookRepository;
import pz.bibliotekabackend.service.BookService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService books;
    private final BookRepository repo;

    @GetMapping
    public ResponseEntity<List<Book>> list(
            @RequestParam(required = false) String rentedBy,
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(required = false, name = "search") String search,

            // paginacija/sort
            @RequestParam(required = false, name = "page") Integer page,
            @RequestParam(required = false, name = "_page") Integer _page,
            @RequestParam(required = false, name = "limit") Integer limit,
            @RequestParam(required = false, name = "_limit") Integer _limit,
            @RequestParam(required = false, name = "sort") String sort,
            @RequestParam(required = false, name = "_sort") String _sort,
            @RequestParam(required = false, name = "order") String order,
            @RequestParam(required = false, name = "_order") String _order,

            @RequestParam(required = false, name = "id") List<String> idsRepeated,
            @RequestParam(required = false, name = "ids") String idsCsv,
            @RequestParam(required = false, name = "ids[]") List<String> idsBracket1,
            @RequestParam(required = false, name = "id[]") List<String> idsBracket2,
            @RequestParam(required = false, name = "favorites") String favoritesCsv,
            @RequestParam(required = false, name = "favorites[]") List<String> favoritesBracket
    ) {
        LinkedHashSet<String> idsSet = new LinkedHashSet<>();

        if (idsRepeated != null) idsSet.addAll(idsRepeated);
        if (idsBracket1 != null) idsSet.addAll(idsBracket1);
        if (idsBracket2 != null) idsSet.addAll(idsBracket2);
        if (favoritesBracket != null) idsSet.addAll(favoritesBracket);

        if (idsCsv != null && !idsCsv.isBlank()) {
            for (String s : idsCsv.split(",")) {
                String t = s.trim();
                if (!t.isBlank()) idsSet.add(t);
            }
        }
        if (favoritesCsv != null && !favoritesCsv.isBlank()) {
            for (String s : favoritesCsv.split(",")) {
                String t = s.trim();
                if (!t.isBlank()) idsSet.add(t);
            }
        }

        if (!idsSet.isEmpty()) {
            List<String> inOrder = new ArrayList<>(idsSet);

            Map<String, Book> byId = new HashMap<>();
            repo.findAllById(inOrder).forEach(b -> byId.put(b.getId(), b));

            List<Book> result = inOrder.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            int n = result.size();
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(n))
                    .header("Content-Range", "items 0-" + (Math.max(0, n - 1)) + "/" + n)
                    .body(result);
        }

        int p = (page != null ? page : (_page != null ? _page : 1));
        int l = (limit != null ? limit : (_limit != null ? _limit : 12));
        String s = (sort != null ? sort : (_sort != null ? _sort : null));
        String o = (order != null ? order : (_order != null ? _order : "asc"));
        String query = (q != null ? q : (search != null ? search : null));

        Page<Book> result = books.page(rentedBy, query, p, l, s, o);

        int from = (p - 1) * l;
        int to = from + result.getNumberOfElements() - 1;
        long total = result.getTotalElements();
        int lastPage = (int) Math.max(1, Math.ceil(total / (double) l));

        String base = "/api/books?sort=" + (s != null ? s : "createdAt")
                + "&order=" + o + "&limit=" + l;
        StringBuilder link = new StringBuilder();
        link.append("<").append(base).append("&page=1").append(">; rel=\"first\"");
        if (p > 1) link.append(", <").append(base).append("&page=").append(p - 1).append(">; rel=\"prev\"");
        if (p < lastPage) link.append(", <").append(base).append("&page=").append(p + 1).append(">; rel=\"next\"");
        link.append(", <").append(base).append("&page=").append(lastPage).append(">; rel=\"last\"");

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(total))
                .header("Content-Range", "items " + (result.getNumberOfElements() == 0 ? 0 : from) + "-" + (result.getNumberOfElements() == 0 ? 0 : to) + "/" + total)
                .header("Link", link.toString())
                .body(result.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Book b = books.get(id);
        return (b != null) ? ResponseEntity.ok(Map.of("data", b)) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Book b) {
        var saved = books.create(b);
        return ResponseEntity.ok(Map.of("data", saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(@PathVariable String id, @RequestBody Map<String, Object> body) {
        var updated = books.patch(id, body);
        return ResponseEntity.ok(Map.of("data", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        books.delete(id);
        return ResponseEntity.noContent().build();
    }
}
