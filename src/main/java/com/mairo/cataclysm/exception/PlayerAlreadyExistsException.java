package com.mairo.cataclysm.exception;

public class PlayerAlreadyExistsException extends CataRuntimeException {

  public PlayerAlreadyExistsException(String userId) {
    super(String.format("Player with given name already exists with id %s", userId));
  }
}
