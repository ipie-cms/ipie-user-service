package in.gov.ipie.service.user.persistence;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import in.gov.ipie.service.user.domain.NotificationChannel;

/**
 * Stores a user's notification-channel preference as a single comma-joined column
 * ({@code "EMAIL,SMS"}) rather than a separate join table - a user's channel set is small,
 * bounded (two values today), and never queried by individual channel, so a join table would add
 * schema/query overhead for no real benefit. Keeps {@link UserJpaEntity#notificationChannels}
 * genuinely typed as {@code Set<NotificationChannel>} rather than scattering CSV parsing at call
 * sites.
 */
@Converter
class NotificationChannelsConverter implements AttributeConverter<Set<NotificationChannel>, String> {

    @Override
    public String convertToDatabaseColumn(Set<NotificationChannel> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<NotificationChannel> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .map(NotificationChannel::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
