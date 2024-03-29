package com.mairo.cataclysm.dto;

import java.time.ZonedDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DistributeLogsReportDto {

  private ZonedDateTime from;
  private ZonedDateTime to;
  private String moderator;
}
