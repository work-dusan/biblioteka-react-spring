package pz.bibliotekabackend.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Data
@Document(collection = "books")
public class Book {

    @MongoId(FieldType.OBJECT_ID)
    private String id;

    private String title;
    private String author;
    private String year;
    private String image;
    private String description;

    @Field(targetType = FieldType.OBJECT_ID)
    private String rentedBy;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
