package uk.gov.justice.laa.ia.datastore.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ItemRequestBody
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-06T09:06:15.352411Z[Europe/London]", comments = "Generator version: 7.18.0")
public class ItemRequestBody implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;

  private String description;

  public ItemRequestBody() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ItemRequestBody(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public ItemRequestBody name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ItemRequestBody description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  @NotNull 
  @Schema(name = "description", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ItemRequestBody itemRequestBody = (ItemRequestBody) o;
    return Objects.equals(this.name, itemRequestBody.name) &&
        Objects.equals(this.description, itemRequestBody.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ItemRequestBody {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
  
  public static class Builder {

    private ItemRequestBody instance;

    public Builder() {
      this(new ItemRequestBody());
    }

    protected Builder(ItemRequestBody instance) {
      this.instance = instance;
    }

    protected Builder copyOf(ItemRequestBody value) { 
      this.instance.setName(value.name);
      this.instance.setDescription(value.description);
      return this;
    }

    public ItemRequestBody.Builder name(String name) {
      this.instance.name(name);
      return this;
    }
    
    public ItemRequestBody.Builder description(String description) {
      this.instance.description(description);
      return this;
    }
    
    /**
    * returns a built ItemRequestBody instance.
    *
    * The builder is not reusable (NullPointerException)
    */
    public ItemRequestBody build() {
      try {
        return this.instance;
      } finally {
        // ensure that this.instance is not reused
        this.instance = null;
      }
    }

    @Override
    public String toString() {
      return getClass() + "=(" + instance + ")";
    }
  }

  /**
  * Create a builder with no initialized field (except for the default values).
  */
  public static ItemRequestBody.Builder builder() {
    return new ItemRequestBody.Builder();
  }

  /**
  * Create a builder with a shallow copy of this instance.
  */
  public ItemRequestBody.Builder toBuilder() {
    ItemRequestBody.Builder builder = new ItemRequestBody.Builder();
    return builder.copyOf(this);
  }

}

