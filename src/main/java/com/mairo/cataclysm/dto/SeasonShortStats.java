package com.mairo.cataclysm.dto;

import lombok.Data;

import java.util.List;

@Data
public class SeasonShortStats {
  private String season;
  private List<PlayerStats> topPlayers;
  private int gamesPlayed;
  private int daysToSeasonEnd;
}