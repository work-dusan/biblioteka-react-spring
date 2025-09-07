package pz.bibliotekabackend.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.List;

@Data
@Document(collection = "users")
public class User {

    @MongoId(FieldType.OBJECT_ID)
    private String id;

    private String name;
    private String email;
    private String password;
    private String role = "user";

    @Field(targetType = FieldType.OBJECT_ID)
    private List<String> favorites;
}
