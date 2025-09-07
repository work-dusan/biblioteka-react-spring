package pz.bibliotekabackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pz.bibliotekabackend.model.Book;
import pz.bibliotekabackend.repository.BookRepository;

import java.time.Instant; // ⬅⬅⬅ DODAJ ovaj import
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository repo;

    public List<Book> list(String rentedBy) {
        return (StringUtils.hasText(rentedBy)) ? repo.findByRentedBy(rentedBy) : repo.findAll();
    }

    public Book get(String id) { return repo.findById(id).orElse(null); }

    public Book create(Book b) {
        if (b.getCreatedAt() == null) {
            b.setCreatedAt(Instant.now());
        }
        return repo.save(b);
    }

    public Book patch(String id, Map<String, Object> changes) {
        Book b = repo.findById(id).orElseThrow();
        if (changes.containsKey("title")) b.setTitle((String) changes.get("title"));
        if (changes.containsKey("author")) b.setAuthor((String) changes.get("author"));
        if (changes.containsKey("year")) b.setYear((String) changes.get("year"));
        if (changes.containsKey("image")) b.setImage((String) changes.get("image"));
        if (changes.containsKey("description")) b.setDescription((String) changes.get("description"));
        if (changes.containsKey("rentedBy")) b.setRentedBy((String) changes.get("rentedBy"));
        return repo.save(b);
    }

    public void delete(String id) { repo.deleteById(id); }

    public Page<Book> page(
            String rentedBy,
            String qOrSearch,
            int page1Based,
            int limit,
            String sortField,
            String order
    ) {
        int pageIdx = Math.max(0, page1Based - 1);
        int size = (limit > 0 && limit <= 200) ? limit : 12;

        String sortBy = (sortField != null && !sortField.isBlank()) ? sortField : "createdAt";
        Sort.Direction dir = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(pageIdx, size, Sort.by(dir, sortBy));

        // rentedBy filter
        if (StringUtils.hasText(rentedBy)) {
            return repo.findByRentedBy(rentedBy.trim(), pageable);
        }

        // search (q ili search)
        if (StringUtils.hasText(qOrSearch)) {
            String q = qOrSearch.trim();
            String rx = ".*" + Pattern.quote(q) + ".*"; // regex "contains"
            return repo.findByTitleRegexIgnoreCaseOrAuthorRegexIgnoreCaseOrYearRegex(rx, rx, rx, pageable);
        }

        // default: sve
        return repo.findAll(pageable);
    }
}
