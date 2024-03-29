package com.mairo.cataclysm.rabbit;

import com.mairo.cataclysm.domain.AuditLog;
import com.mairo.cataclysm.domain.Player;
import com.mairo.cataclysm.dto.BotInputMessage;
import com.mairo.cataclysm.dto.OutputMessage;
import com.mairo.cataclysm.dto.StoreAuditLogDto;
import com.mairo.cataclysm.exception.InvalidCommandException;
import com.mairo.cataclysm.postprocessor.PostProcessor;
import com.mairo.cataclysm.processor.CommandProcessor;
import com.mairo.cataclysm.properties.AppProps;
import com.mairo.cataclysm.properties.RabbitProps;
import com.mairo.cataclysm.service.AuditLogService;
import com.mairo.cataclysm.service.UserRightsService;
import com.mairo.cataclysm.utils.Commands;
import com.rabbitmq.client.ConnectionFactory;
import java.time.Duration;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.util.retry.RetrySpec;

@RequiredArgsConstructor
@Service
public class CommandReceiver {

  private static final Logger log = LogManager.getLogger(CommandReceiver.class);

  private final MetadataParser metadataParser;
  private final RabbitSender rabbitSender;
  private final ConnectionFactory connectionFactory;
  private final RabbitProps rabbitProps;
  private final AppProps appProps;
  private final List<CommandProcessor> processors;
  private final List<PostProcessor> postProcessors;
  private final UserRightsService userRightsService;
  private final AuditLogService auditLogService;

  @PostConstruct
  public void init() {
    ReceiverOptions receiverOptions = new ReceiverOptions()
        .connectionFactory(connectionFactory)
        .connectionMonoConfigurator(cm -> cm.retryWhen(RetrySpec.backoff(3, Duration.ofSeconds(3))))
        .connectionSupplier(cf -> cf.newConnection("cata_input_receiver_conn"));

    Receiver receiver = RabbitFlux.createReceiver(receiverOptions);

    receiver.consumeAutoAck(rabbitProps.getInputQueue())
        .publishOn(Schedulers.elastic())
        .flatMap(delivery -> metadataParser.parseCommand(delivery.getBody()))
        .flatMap(this::runProcessor)
        .subscribe();
  }

  private Mono<List<OutputMessage>> runProcessor(BotInputMessage input) {
    return
        createAuditLog(input)
            .then(checkUserIsRegistered(input))
            .then(processCmd(input))
            .onErrorResume(e -> transformError(e, input))
            .flatMap(rabbitSender::send)
            .flatMap(output -> runPostProcess(input, output));
  }

  private Mono<AuditLog> createAuditLog(BotInputMessage input) {
    String msg = String.format("/%s was called by %s (%s)", input.getCmd(), input.getUser(), input.getTid());
    return auditLogService.storeAuditLog(new StoreAuditLogDto(msg))
        .doOnNext(al -> log.info(msg));
  }

  private Mono<Player> checkUserIsRegistered(BotInputMessage input) {
    if (input.getCmd().equals(Commands.STORE_LOG_CMD)) {
      return Mono.empty();
    } else {
      return userRightsService.checkUserIsRegistered(input.getTid());
    }
  }

  private Mono<OutputMessage> transformError(Throwable e, BotInputMessage input) {
    return Mono.just(OutputMessage.error(input.getChatId(), msgId(), format(e)))
        .doOnNext((msg) -> log.error(e.getMessage()));
  }

  private Mono<List<OutputMessage>> runPostProcess(BotInputMessage input, OutputMessage output) {
    return output.isError() ? Mono.empty() : postProcess(input);
  }

  private Mono<List<OutputMessage>> postProcess(BotInputMessage input) {
    if (appProps.isNotificationsEnabled()) {
      return postProcessors.stream()
          .filter(pp -> pp.commands().contains(input.getCmd()))
          .findAny()
          .map(pp -> pp.postProcess(input, msgId()).collectList())
          .orElseGet(Mono::empty);
    }
    return Mono.empty();
  }

  private Mono<OutputMessage> processCmd(BotInputMessage dto) {
    return processors.stream()
        .filter(p -> p.commands().contains(dto.getCmd()))
        .findFirst()
        .map(p -> p.process(dto, msgId()))
        .orElseGet(() -> Mono.error(new InvalidCommandException(dto.getCmd())));
  }

  private int msgId() {
    return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
  }

  public String format(Throwable error) {
    return String.format("%sERROR: %s%s", CommandProcessor.PREFIX, error.getMessage(), CommandProcessor.SUFFIX);
  }
}
