package pz.bibliotekabackend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pz.bibliotekabackend.model.User;
import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {
    List<User> findByEmail(String email);
}
