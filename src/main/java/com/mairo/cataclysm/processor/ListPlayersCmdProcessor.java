package com.mairo.cataclysm.processor;

import com.mairo.cataclysm.dto.BotInputMessage;
import com.mairo.cataclysm.dto.FoundAllPlayers;
import com.mairo.cataclysm.dto.OutputMessage;
import com.mairo.cataclysm.model.PlayerModel;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ListPlayersCmdProcessor implements CommandProcessor {

  private final PlayerModel playerModel;

  @Override
  public Mono<OutputMessage> process(BotInputMessage input, int msgId) {
    return playerModel.findAllPlayers()
        .map(this::format)
        .map(str -> OutputMessage.ok(input.getChatId(), msgId, str));
  }

  @Override
  public List<String> commands() {
    return List.of(LIST_PLAYERS_CMD);
  }

  private String format(FoundAllPlayers data) {
    String players = data.getPlayers().stream()
        .map(p -> String.format("%s|%s", p.getId(), StringUtils.capitalize(p.getSurname())))
        .collect(Collectors.joining("\n"));
    return String.format("%s%s%s", PREFIX, players, SUFFIX);
  }

}
