package com.here.xyz.models.hub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.here.xyz.View;

@JsonTypeName(value = "Not")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConstraintNot {

  /**
   * The constraints that should be negated.
   */
  @JsonProperty
  @JsonView(View.All.class)
  public Constraint of;
}