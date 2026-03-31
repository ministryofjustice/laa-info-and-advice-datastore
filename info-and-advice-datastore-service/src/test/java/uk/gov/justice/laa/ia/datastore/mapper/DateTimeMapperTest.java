package uk.gov.justice.laa.ia.datastore.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Tests for the date time mapper. */
public class DateTimeMapperTest {

  private DateTimeMapper sut = new DateTimeMapperImpl();
  private static Instant instant = Instant.ofEpochSecond(1774868400);
  private static OffsetDateTime offsetDateTime =
      OffsetDateTime.of(2026, 03, 30, 11, 0, 0, 0, ZoneOffset.UTC);

  @Test
  void toInstant_shouldMapCorrectly() {
    assertThat(sut.map(offsetDateTime)).isEqualTo(instant);
  }

  @Test
  void toInstant_whenNull_shouldReturnNull() {
    assertNull(sut.map((Instant) null));
  }

  @Test
  void toOffsetDateTime_shouldMapCorrectly() {
    assertThat(sut.map(instant)).isEqualTo(offsetDateTime);
  }

  @Test
  void toOffsetDateTime_whenNull_shouldReturnNull() {
    assertNull(sut.map((OffsetDateTime) null));
  }
}
