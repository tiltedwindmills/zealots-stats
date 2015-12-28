package org.tiltedwindmills.fantasy.zealots;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tiltedwindmills.fantasy.mfl.model.League;
import org.tiltedwindmills.fantasy.mfl.model.Player;
import org.tiltedwindmills.fantasy.mfl.model.Position;
import org.tiltedwindmills.fantasy.mfl.model.weeklyresults.MatchupResults;
import org.tiltedwindmills.fantasy.mfl.model.weeklyresults.PlayerResultDetails;
import org.tiltedwindmills.fantasy.mfl.model.weeklyresults.TeamResultDetails;
import org.tiltedwindmills.fantasy.mfl.model.weeklyresults.WeeklyResultsResponse;
import org.tiltedwindmills.fantasy.mfl.model.weeklyresults.WeeklyResultsWrapper;
import org.tiltedwindmills.fantasy.mfl.services.FranchiseService;
import org.tiltedwindmills.fantasy.mfl.services.LeagueService;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

@Controller
public class WeeklyResultsController {

    private static final Logger LOG = LoggerFactory.getLogger(WeeklyResultsController.class);

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
    @RequestMapping("/")
    public final String home(final Map<String, Object> model, final HttpSession session) {

        final Map<Position, Double> positionScoreMap = new HashMap<>();

        for (League league : propertyBasedLeagues) {
            for (int i = 1; i <= 13; i++) {
                LOG.debug("Loading week {} for {}", i, league.getName());
                mapWeeklyScoresForLeagueWeek(positionScoreMap, league, i);
            }
        }

        model.put("positionScoreMap", positionScoreMap);
        return "index";
    }


    private void mapWeeklyScoresForLeagueWeek(final Map<Position, Double> positionScoreMap, final League league, int week) {

        final WeeklyResultsWrapper weeklyResultsWrapper = getWeeklyResultsFromFile(league.getName(), week);

        if (weeklyResultsWrapper != null && weeklyResultsWrapper.getMatchupResults() != null) {

            for (MatchupResults matchupResults : weeklyResultsWrapper.getMatchupResults()) {
                loadMatchupResults(positionScoreMap, matchupResults);
            }
        }
    }

    private void loadMatchupResults(Map<Position, Double> positionScoreMap, final MatchupResults matchupResults) {

        if (matchupResults != null && matchupResults.getTeams() != null) {

            for (TeamResultDetails teamResultDetails : matchupResults.getTeams()) {
                loadTeamMatchupResults(positionScoreMap, teamResultDetails);
            }
        }
    }

    private void loadTeamMatchupResults(Map<Position, Double> positionScoreMap, final TeamResultDetails teamResultDetails) {

        if (teamResultDetails != null) {

            for(final PlayerResultDetails playerResultDetails : teamResultDetails.getPlayerResults()) {

                if (playerResultDetails != null && "starter".equals(playerResultDetails.getStatus())) {

                    Player player = Iterables.find(players, new Predicate<Player>() {

                        public boolean apply(Player testPlayer) {
                            return testPlayer != null && testPlayer.getId() == playerResultDetails.getPlayerId();
                        }
                    });

                    LOG.trace("Adding {} score of {} to map for {}",
                            player.getPosition(), playerResultDetails.getScore(), player.getName());
                    addPlayerScoreToMap(positionScoreMap, player.getPosition(), playerResultDetails.getScore());
                }
            }
        }
    }

    private WeeklyResultsWrapper getWeeklyResultsFromFile(String name, int week) {

        final String fileName = name + "week" + week + ".json";

        try {
            final Resource resource = new ClassPathResource("data/weeklyResults/" + fileName);
            final InputStream resourceInputStream = resource.getInputStream();

            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            final WeeklyResultsResponse response =
                    objectMapper.readValue(resourceInputStream, WeeklyResultsResponse.class);

            if (response != null) {
                return response.getWeeklyResults();
            }

        } catch (IOException e) {
            LOG.error("Failed to load MFL players from file: {}", e.getMessage());
        }

        return null;
    }


    private void addPlayerScoreToMap(Map<Position, Double> positionScoreMap, Position position, double score) {

        if (positionScoreMap == null) {
            positionScoreMap = new HashMap<>();
        }

        Position mappedPosition = position;
        if (position == Position.CORNERBACK || position == Position.SAFETY) {
            mappedPosition = Position.DEFENSIVE_BACK;
        }

        if (position == Position.DEFENSIVE_TACKLE || position == Position.DEFENSIVE_END) {
            mappedPosition = Position.DEFENSIVE_LINEMAN;
        }

        if (positionScoreMap.containsKey(mappedPosition)) {
            positionScoreMap.put(mappedPosition, positionScoreMap.get(mappedPosition) + score);

        } else {
            positionScoreMap.put(mappedPosition, score);
        }
    }
}
