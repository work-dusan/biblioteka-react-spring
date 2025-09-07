package pz.bibliotekabackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pz.bibliotekabackend.model.User;
import pz.bibliotekabackend.repository.BookRepository;
import pz.bibliotekabackend.repository.OrderRepository;
import pz.bibliotekabackend.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository repo;
    @Mock BookRepository bookRepo;
    @Mock OrderRepository orderRepo;

    // koristimo pravi encoder da potvrdimo da je hashovan
    @Spy BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @InjectMocks UserService service;

    @Test
    void create_hashesPlainPassword() {
        User u = new User();
        u.setName("Marko");
        u.setEmail("marko@example.com");
        u.setPassword("tajna");

        given(repo.save(Mockito.any(User.class)))
                .willAnswer(inv -> inv.getArgument(0));

        User saved = service.create(u);

        assertThat(saved.getPassword())
                .startsWith("$2a$")
                .isNotEqualTo("tajna");
    }
}
