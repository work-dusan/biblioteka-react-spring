package pz.bibliotekabackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new StringToInstantConverter()));
    }

    @ReadingConverter
    static class StringToInstantConverter implements Converter<String, Instant> {
        private static final DateTimeFormatter LEGACY =
                DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

        @Override
        public Instant convert(String source) {
            if (source == null || source.isBlank()) return null;

            // 1) ISO INSTANT (npr. 2025-08-16T15:41:37Z)
            try { return Instant.parse(source); } catch (Exception ignored) {}

            // 2) ISO sa offsetom (npr. 2025-08-16T15:41:37.123+00:00)
            try { return OffsetDateTime.parse(source, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant(); }
            catch (Exception ignored) {}

            // 3) Legacy "Sat Aug 16 17:41:37 CEST 2025"
            try { return java.time.ZonedDateTime.parse(source, LEGACY).toInstant(); }
            catch (Exception ignored) {}

            try {
                var sdf = new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
                return sdf.parse(source).toInstant();
            } catch (Exception e) {
                throw new IllegalArgumentException("Ne mogu da parsiram datum: " + source, e);
            }
        }
    }
}
