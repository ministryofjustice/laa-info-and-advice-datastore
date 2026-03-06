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
 * Item
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-06T09:06:15.352411Z[Europe/London]", comments = "Generator version: 7.18.0")
public class Item implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private String name;

  private String description;

  public Item() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Item(Long id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public Item id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Item name(String name) {
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

  public Item description(String description) {
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
    Item item = (Item) o;
    return Objects.equals(this.id, item.id) &&
        Objects.equals(this.name, item.name) &&
        Objects.equals(this.description, item.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Item {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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

    private Item instance;

    public Builder() {
      this(new Item());
    }

    protected Builder(Item instance) {
      this.instance = instance;
    }

    protected Builder copyOf(Item value) { 
      this.instance.setId(value.id);
      this.instance.setName(value.name);
      this.instance.setDescription(value.description);
      return this;
    }

    public Item.Builder id(Long id) {
      this.instance.id(id);
      return this;
    }
    
    public Item.Builder name(String name) {
      this.instance.name(name);
      return this;
    }
    
    public Item.Builder description(String description) {
      this.instance.description(description);
      return this;
    }
    
    /**
    * returns a built Item instance.
    *
    * The builder is not reusable (NullPointerException)
    */
    public Item build() {
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
  public static Item.Builder builder() {
    return new Item.Builder();
  }

  /**
  * Create a builder with a shallow copy of this instance.
  */
  public Item.Builder toBuilder() {
    Item.Builder builder = new Item.Builder();
    return builder.copyOf(this);
  }

}

