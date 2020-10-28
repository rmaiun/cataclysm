package com.mairo.cataclysm.formatter;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import com.mairo.cataclysm.dto.FoundLastRounds;
import com.mairo.cataclysm.dto.FullRound;
import com.mairo.cataclysm.utils.DateUtils;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class LastRoundsMessageFormatter implements MessageFormatter<FoundLastRounds> {

  @Override
  public String format(FoundLastRounds data) {

    if (isEmpty(data.getRounds())) {
      return String.format("%s There are no games in season %s%s", PREFIX, data.getSeason(), SUFFIX);
    } else {
      return data.getRounds().stream()
          .map(this::formatRound)
          .collect(Collectors.joining(DELIMITER, PREFIX, SUFFIX));
    }
  }

  private String formatRound(FullRound round) {
    String date = DateUtils.formatDateWithHour(round.getCreated());
    String winners = String.format("%s/%s", round.getWinner1(), round.getWinner2());
    String losers = String.format("%s/%s", round.getLoser1(), round.getLoser2());
    StringBuilder sb = new StringBuilder();
    sb.append("date: ").append(date).append(LINE_SEPARATOR);
    sb.append("winners: ").append(winners).append(LINE_SEPARATOR);
    sb.append("losers: ").append(losers).append(LINE_SEPARATOR);
    if (round.isShutout()) {
      sb.append("shutout: ✓").append(LINE_SEPARATOR);
    }
    return sb.toString();
  }
}
