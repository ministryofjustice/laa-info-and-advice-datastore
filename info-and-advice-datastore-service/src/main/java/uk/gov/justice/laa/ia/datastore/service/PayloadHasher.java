package uk.gov.justice.laa.ia.datastore.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** Computes a SHA-256 hash of a JSON payload, used to detect duplicate event submissions. */
@Component
public class PayloadHasher {

  /**
   * Hashes the given payload.
   *
   * @param payload the JSON payload to hash
   * @return the hex-encoded SHA-256 hash of the payload's JSON representation
   */
  public String hash(JsonNode payload) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(payload.toString().getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
