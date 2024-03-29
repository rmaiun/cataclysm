package com.mairo.cataclysm.controller;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mairo.cataclysm.domain.Player;
import com.mairo.cataclysm.dto.AddPlayerDto;
import com.mairo.cataclysm.dto.FoundAllPlayers;
import com.mairo.cataclysm.dto.IdDto;
import com.mairo.cataclysm.exception.CataRuntimeException;
import com.mairo.cataclysm.repository.PlayerRepository;
import com.mairo.cataclysm.service.UserRightsService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlayerControllerTest {

  @Autowired
  private ApplicationContext ctx;

  @MockBean
  PlayerRepository repository;
  @MockBean
  UserRightsService userRightsService;

  private static WebTestClient webClient;

  @BeforeEach
  void setup() {
    webClient = WebTestClient.bindToApplicationContext(ctx).build();
  }

  @Test
  void playersAllTest() {
    Player p = new Player();
    p.setId("1234");
    p.setSurname("test");
    when(repository.listAll()).thenReturn(Mono.just(List.of(p)));

    webClient.get()
        .uri("/players/all")
        .exchange()
        .expectStatus().isOk()
        .expectBody(FoundAllPlayers.class)
        .consumeWith(res -> {
          assertNotNull(res.getResponseBody());
          assertEquals(1, res.getResponseBody().getPlayers().size());
          assertEquals(p.getSurname(), res.getResponseBody().getPlayers().get(0).getSurname());
        });
    verify(repository, times(1)).listAll();
  }

  @Test
  void playersAddTest() {
    Player p = new Player();
    p.setId("1234");
    p.setSurname("test");
    when(repository.getPlayer(anyString())).thenReturn(Mono.just(Optional.empty()));
    when(repository.savePlayer(any(Player.class))).thenReturn(Mono.just(p));
    when(userRightsService.checkUserIsAdmin(eq("1111"))).thenReturn(Mono.empty());
    webClient.post()
        .uri("/players/add")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(new AddPlayerDto("Testuser", "1444",false, "1111")))
        .exchange()
        .expectStatus().isOk()
        .expectBody(IdDto.class)
        .consumeWith(res -> {
          assertNotNull(res.getResponseBody());
          assertEquals("1234", res.getResponseBody().getId());
        });
  }

  @Test
  void playersAddTestUserExistsException() {
    Player p = new Player();
    p.setId("1234");
    p.setSurname("test");
    when(repository.getPlayer(anyString())).thenReturn(Mono.just(Optional.of(new Player("30L","test","1",false,false))));
    when(repository.savePlayer(any(Player.class))).thenReturn(Mono.just(p));
    when(userRightsService.checkUserIsAdmin(eq("1111"))).thenReturn(Mono.empty());

    webClient.post()
        .uri("/players/add")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(new AddPlayerDto("Testuser", "1444",false,"1111")))
        .exchange()
        .expectStatus().is4xxClientError()
        .expectBody(CataRuntimeException.class);
  }
}
