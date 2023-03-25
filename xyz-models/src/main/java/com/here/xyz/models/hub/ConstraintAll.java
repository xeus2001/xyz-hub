package com.here.xyz.models.hub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.here.xyz.View.All;
import java.util.List;
import org.jetbrains.annotations.NotNull;

@JsonTypeName(value = "All")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConstraintAll {

  /**
   * The constraints that all need to hold true (AND).
   */
  @JsonProperty
  @JsonView(All.class)
  public List<@NotNull Constraint> of;
}
