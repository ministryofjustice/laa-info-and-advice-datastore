package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PayloadHasher}. */
class PayloadHasherTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PayloadHasher sut = new PayloadHasher();

  @Test
  void hash_shouldReturnSameHash_forIdenticalPayloads() {
    JsonNode first = objectMapper.createObjectNode().put("ufn", "123456/1");
    JsonNode second = objectMapper.createObjectNode().put("ufn", "123456/1");

    assertThat(sut.hash(first)).isEqualTo(sut.hash(second));
  }

  @Test
  void hash_shouldReturnDifferentHash_forDifferentPayloads() {
    JsonNode first = objectMapper.createObjectNode().put("ufn", "123456/1");
    JsonNode second = objectMapper.createObjectNode().put("ufn", "123456/2");

    assertThat(sut.hash(first)).isNotEqualTo(sut.hash(second));
  }

  @Test
  void hash_shouldReturnA64CharacterHexString() {
    JsonNode payload = objectMapper.createObjectNode().put("ufn", "123456/1");

    assertThat(sut.hash(payload)).hasSize(64).matches("[0-9a-f]+");
  }
}
