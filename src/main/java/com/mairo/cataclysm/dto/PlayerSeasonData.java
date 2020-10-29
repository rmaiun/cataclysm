package com.mairo.cataclysm.dto;

import com.mairo.cataclysm.domain.Season;
import java.util.Map;
import lombok.Value;
import lombok.With;

@Value
@With
public class PlayerSeasonData {

  Season season;
  Map<Long, String> players;
}
