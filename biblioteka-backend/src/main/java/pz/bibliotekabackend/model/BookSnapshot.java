package pz.bibliotekabackend.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Data
public class BookSnapshot {

    @Field(targetType = FieldType.OBJECT_ID)
    private String id;

    private String title;
    private String author;
    private String year;
    private String image;
}
