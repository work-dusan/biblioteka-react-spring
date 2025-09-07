package pz.bibliotekabackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pz.bibliotekabackend.model.Book;
import pz.bibliotekabackend.model.Order;
import pz.bibliotekabackend.repository.BookRepository;
import pz.bibliotekabackend.repository.OrderRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepo;
    @Mock BookRepository bookRepo;

    @InjectMocks OrderService service;

    @Test
    void create_withExplicitBookId_buildsSnapshot_andMarksRented() {
        // given
        var inbound = new Order();
        inbound.setUserId("U1");
        inbound.setBookId("B1");

        var book = new Book();
        book.setId("B1");
        book.setTitle("Clean Code");
        book.setAuthor("Robert C. Martin");
        book.setYear("2008");
        // book.setRentedBy(null);  // default is null

        given(bookRepo.findById("B1")).willReturn(Optional.of(book));
        // vrati isti entity sa upisanim ID-jem (simuliramo save u repo)
        given(orderRepo.save(Mockito.any(Order.class)))
                .willAnswer(inv -> {
                    Order o = inv.getArgument(0);
                    if (o.getId() == null) o.setId("O1");
                    return o;
                });
        given(bookRepo.save(Mockito.any(Book.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Order saved = service.create(inbound);

        // then
        assertThat(saved.getId()).isEqualTo("O1");
        assertThat(saved.getUserId()).isEqualTo("U1");
        assertThat(saved.getBookId()).isEqualTo("B1");
        assertThat(saved.getStatus()).isEqualTo("active");
        assertThat(saved.getRentedAt()).isNotNull();
        assertThat(saved.getBookSnapshot()).isNotNull();
        assertThat(saved.getBookSnapshot().getTitle()).isEqualTo("Clean Code");

        // knjiga obeležena kao iznajmljena od U1
        ArgumentCaptor<Book> bookCap = ArgumentCaptor.forClass(Book.class);
        verify(bookRepo).save(bookCap.capture());
        assertThat(bookCap.getValue().getRentedBy()).isEqualTo("U1");
    }

    @Test
    void create_withoutBookId_throwsIllegalArgument() {
        // given
        var inbound = new Order();
        inbound.setUserId("U1");
        inbound.setBookId(null);

        // u servisu se proverava i pokušava da se zaključi poslednja knjiga po rentedBy;
        // simuliramo da takve knjige nema
        given(bookRepo.findTopByRentedByOrderByUpdatedAtDesc("U1")).willReturn(Optional.empty());
        given(bookRepo.findTopByRentedByOrderByCreatedAtDesc("U1")).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.create(inbound))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId i bookId su obavezni");
        verify(orderRepo, never()).save(Mockito.any());
    }

    @Test
    void returnOrder_setsReturnedAndFreesBook() {
        // given
        var order = new Order();
        order.setId("O1");
        order.setUserId("U1");
        order.setBookId("B1");
        order.setStatus("active");
        order.setRentedAt(Instant.now());

        var book = new Book();
        book.setId("B1");
        book.setTitle("DDD");
        book.setAuthor("Evans");
        book.setYear("2003");
        book.setRentedBy("U1");

        given(orderRepo.findById("O1")).willReturn(Optional.of(order));
        given(orderRepo.save(Mockito.any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        given(bookRepo.findById("B1")).willReturn(Optional.of(book));
        given(bookRepo.save(Mockito.any(Book.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Order updated = service.returnOrder("O1");

        // then
        assertThat(updated.getStatus()).isEqualTo("returned");
        assertThat(updated.getReturnedAt()).isNotNull();

        ArgumentCaptor<Book> bookCap = ArgumentCaptor.forClass(Book.class);
        verify(bookRepo).save(bookCap.capture());
        assertThat(bookCap.getValue().getRentedBy()).isNull();
    }
}
