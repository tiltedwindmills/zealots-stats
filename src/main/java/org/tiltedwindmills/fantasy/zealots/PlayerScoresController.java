package org.tiltedwindmills.fantasy.zealots;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

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
import org.tiltedwindmills.fantasy.zealots.model.PlayerScoreBreakdown;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
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

        final List<PlayerScoreBreakdown> playerScoreBreakdowns = new ArrayList<>();

        final Position position = Position.fromValue(positionName);
        if (position != Position.UNKNOWN) {

            for (int i = 1; i <= 13; i++) {
                LOG.debug("Loading week {} for {}", i, position);
                getPlayerScoresForWeek(playerScoreBreakdowns, position, i, getLeagueStarterLimit(position));
            }
        }

        Ordering<PlayerScoreBreakdown> byTopFinishes = new Ordering<PlayerScoreBreakdown>() {

            @Override
            public int compare(PlayerScoreBreakdown left, PlayerScoreBreakdown right) {
                int gradeDiff = right.getGrade() - left.getGrade();
                if (gradeDiff != 0) {
                    return gradeDiff;
                }
                int top12diff = right.getTop12Finishes() - left.getTop12Finishes();
                if (top12diff != 0) {
                    return top12diff;
                }
                int top24diff = right.getTop24Finishes() - left.getTop24Finishes();
                if (top24diff != 0) {
                    return top24diff;
                }
                //int top36diff = right.getTop36Finishes() - left.getTop36Finishes();
                //if (top36diff != 0) {
                //    return top36diff;
                //}

                return left.getName().compareTo(right.getName());
            }
        };

        Collections.sort(playerScoreBreakdowns, byTopFinishes);

        model.put("playerScoreBreakdowns", playerScoreBreakdowns);
        return "starterWeeks";
    }


    private void getPlayerScoresForWeek(List<PlayerScoreBreakdown> playerScoreBreakdowns, Position position, int week, int limit) {

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


                    Optional<PlayerScoreBreakdown> existingPlayerScoreBreakdown =
                            Iterables.tryFind(playerScoreBreakdowns, new Predicate<Player>() {

                        public boolean apply(Player testPlayer) {
                            return testPlayer != null && testPlayer.getId() == playerScore.getPlayerId();
                        }
                    });

                    if (existingPlayerScoreBreakdown.isPresent()) {
                        existingPlayerScoreBreakdown.get().addTopFinish(i);

                    } else {
                        PlayerScoreBreakdown newPlayerScoreBreakdown = new PlayerScoreBreakdown(player);
                        newPlayerScoreBreakdown.addTopFinish(i);
                        playerScoreBreakdowns.add(newPlayerScoreBreakdown);
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
