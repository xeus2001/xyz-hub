package com.here.xyz.models.hub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.here.xyz.View;

/**
 * A commit messages attached to a transaction.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionCommitMessage extends TransactionElement {

  /**
   * A commit message.
   */
  @JsonProperty
  @JsonView(View.All.class)
  public String commit_msg;

  /**
   * The commit JSON.
   */
  @JsonProperty
  @JsonView(View.All.class)
  public Object commit_json;

}