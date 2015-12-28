package org.tiltedwindmills.fantasy.zealots;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tiltedwindmills.fantasy.mfl.model.League;
import org.tiltedwindmills.fantasy.mfl.model.Player;
import org.tiltedwindmills.fantasy.mfl.model.Position;
import org.tiltedwindmills.fantasy.mfl.model.players.PlayerScore;
import org.tiltedwindmills.fantasy.mfl.model.players.PlayerScoresResponse;
import org.tiltedwindmills.fantasy.mfl.model.players.PlayerScoresWrapper;
import org.tiltedwindmills.fantasy.mfl.services.LeagueService;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

@Controller
public class PlayerScoresController {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerScoresController.class);

    @Inject
    private LeagueService leagueService;

    @javax.annotation.Resource
    private List<League> propertyBasedLeagues;

    @javax.annotation.Resource
    private List<Player> players;

    @PostConstruct
    private void postConstruct() {

        checkNotNull(leagueService, "leagueService cannot be null");
    }


    @SuppressWarnings("unchecked")
    @RequestMapping("/starterWeeks/{positionName}")
    public final String starterWeeks(final @PathVariable String positionName, final Map<String, Object> model) {

        final Comparator<Player> playerComparator = new Comparator<Player>() {

            @Override
            public int compare(Player o1, Player o2) {
                return ObjectUtils.compare(o1.getName(), o2.getName());
            }
        };

        final Map<Player, Integer> playerStarterMap = new TreeMap<>(playerComparator);

        final Position position = Position.fromValue(positionName);
        if (position != position.UNKNOWN) {

            for (int i = 1; i <= 13; i++) {
                LOG.debug("Loading week {} for {}", i, position);
                getPlayerScoresForWeek(playerStarterMap, position, i, getLeagueStarterLimit(position));
            }
        }

        Ordering<Map.Entry<Player, Integer>> byMapValues = new Ordering<Map.Entry<Player, Integer>>() {

           @Override
           public int compare(Map.Entry<Player, Integer> left, Map.Entry<Player, Integer> right) {
                return left.getValue().compareTo(right.getValue());
           }
        };

        List<Map.Entry<Player, Integer>> keys = Lists.newArrayList(playerStarterMap.entrySet());
        Collections.sort(keys, byMapValues.reverse());

        model.put("playerStarterMap", keys);
        return "starterWeeks";
    }


    private void getPlayerScoresForWeek(Map<Player, Integer> playerStarterMap, Position position, int week, int limit) {

        final PlayerScoresWrapper playerScoresWrapper = getPlayerScoresFromFile(position, week);

        if (playerScoresWrapper != null && playerScoresWrapper.getPlayerScores() != null) {

            for (int i = 0; i < limit; i++) {

                final PlayerScore playerScore = playerScoresWrapper.getPlayerScores().get(i);
                if (playerScore != null) {

                    Player player = Iterables.find(players, new Predicate<Player>() {

                        public boolean apply(Player testPlayer) {
                            return testPlayer != null && testPlayer.getId() == playerScore.getPlayerId();
                        }
                    });

                    if (playerStarterMap.containsKey(player)) {
                        playerStarterMap.put(player, playerStarterMap.get(player) + 1);

                    } else {
                        playerStarterMap.put(player, 1);
                    }
                }
            }
        }
    }

    private PlayerScoresWrapper getPlayerScoresFromFile(Position position, int week) {

        final String fileName = position.getType() + "_week" + week + ".json";

        try {
            final Resource resource = new ClassPathResource("data/scores/" + fileName);
            final InputStream resourceInputStream = resource.getInputStream();

            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            final PlayerScoresResponse response =
                    objectMapper.readValue(resourceInputStream, PlayerScoresResponse.class);

            if (response != null) {
                return response.getWrapper();
            }

        } catch (IOException e) {
            LOG.error("Failed to load MFL players from file: {}", e.getMessage());
        }

        return null;
    }


    private int getLeagueStarterLimit(Position position) {

        int limit = -1;

        switch (position) {
            case QUARTERBACK:
            case TIGHT_END:
            case KICKER:
                limit = 12;
                break;

            case RUNNING_BACK:
                limit = 24;
                break;

            case WIDE_RECEIVER:
            case DEFENSIVE_BACK:
            case LINEBACKER:
            case DEFENSIVE_LINEMAN:
                limit = 36;
                break;

            default:
                break;
        }

        return limit;
    }
}
