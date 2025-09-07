package pz.bibliotekabackend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pz.bibliotekabackend.model.Order;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByUserId(String userId);
    List<Order> findByBookId(String bookId);

    long deleteByUserId(String userId);
}
