package pz.bibliotekabackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import pz.bibliotekabackend.model.Book;
import java.util.List;
import java.util.Optional;

public interface BookRepository extends MongoRepository<Book, String> {
    List<Book> findByRentedBy(String rentedBy); // za filter ?rentedBy=

    Page<Book> findByRentedBy(String rentedBy, Pageable pageable);

    Page<Book> findByTitleRegexIgnoreCaseOrAuthorRegexIgnoreCaseOrYearRegex(
            String titleRegex, String authorRegex, String yearRegex, Pageable pageable
    );

    Optional<Book> findTopByRentedByOrderByUpdatedAtDesc(String rentedBy);
    Optional<Book> findTopByRentedByOrderByCreatedAtDesc(String rentedBy);

}