package pz.bibliotekabackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Data
@Document(collection = "orders")
public class Order {

    @MongoId(FieldType.OBJECT_ID)
    private String id;

    @Field(targetType = FieldType.OBJECT_ID)
    private String userId;

    @Field(targetType = FieldType.OBJECT_ID)
    private String bookId;

    private BookSnapshot bookSnapshot;

    @Field("book")
    @JsonProperty("book")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BookSnapshot legacyBook;

    private String status;
    private Instant rentedAt;
    private Instant returnedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Field("__v")
    private Long version;

    // ======== displayBook: samo za response (ne upisuje se u bazu) ========
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DisplayBook displayBook;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DisplayBook {
        private String id;
        private String title;
        private String author;
        private String year;
        private String image;

        public static DisplayBook from(BookSnapshot s) {
            if (s == null) return null;
            DisplayBook d = new DisplayBook();
            d.setId(s.getId());
            d.setTitle(s.getTitle());
            d.setAuthor(s.getAuthor());
            d.setYear(s.getYear());
            d.setImage(s.getImage());
            return d;
        }
    }
}
