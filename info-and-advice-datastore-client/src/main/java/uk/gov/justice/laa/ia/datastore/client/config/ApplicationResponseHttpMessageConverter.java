package uk.gov.justice.laa.ia.datastore.client.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;
import org.openapitools.jackson.nullable.JsonNullableJackson3Module;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.justice.laa.ia.datastore.client.model.ApplicationResponse;

public final class ApplicationResponseHttpMessageConverter extends JacksonJsonHttpMessageConverter {

  private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
  private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

  public ApplicationResponseHttpMessageConverter() {
    this(JsonMapper.builder());
  }

  @Override
  public boolean canRead(Class<?> clazz, MediaType mediaType) {
    return clazz == ApplicationResponse.class && super.canRead(clazz, mediaType);
  }

  @Override
  public boolean canRead(ResolvableType type, MediaType mediaType) {
    return type.resolve() == ApplicationResponse.class && super.canRead(type, mediaType);
  }

  @Override
  public boolean canWrite(Class<?> clazz, MediaType mediaType) {
    return false;
  }

  @Override
  public boolean canWrite(ResolvableType type, Class<?> clazz, MediaType mediaType) {
    return false;
  }

  private ApplicationResponseHttpMessageConverter(JsonMapper.Builder mapperBuilder) {
    super(
        mapperBuilder
            .addModule(new JsonNullableJackson3Module())
            .addMixIn(ApplicationResponse.class, ApplicationResponseMixin.class));
  }

  private abstract static class ApplicationResponseMixin {
    @JsonProperty("eTag")
    @JsonDeserialize(using = VersionDeserializer.class)
    abstract void seteTag(Long eTag);
  }

  private static final class VersionDeserializer extends ValueDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context)
        throws JacksonException {
      if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
        parser.skipChildren();
        return null;
      }

      BigInteger version = parser.getBigIntegerValue();
      if (version.compareTo(LONG_MIN) < 0 || version.compareTo(LONG_MAX) > 0) {
        return null;
      }
      return version.longValue();
    }
  }
}
