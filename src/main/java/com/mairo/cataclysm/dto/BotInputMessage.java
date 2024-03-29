package com.mairo.cataclysm.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BotInputMessage {

  private String cmd;
  private String chatId;
  private String tid;
  private String user;
  private Map<String, Object> data = new HashMap<>();
}
