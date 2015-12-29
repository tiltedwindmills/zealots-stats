package org.tiltedwindmills.fantasy.zealots;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.tiltedwindmills.fantasy.mfl.services.LeagueService;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Let's figure out how many of each position were on rosters.  We'll use the weekly results export
 * since the generic rosters will represent off-season configuration.  ( week 13 seems good since
 * generally all the owners will still care).
 *
 *
 * @author John Daniel
 */
@Controller
public class RosteredPlayersController {

    private static final Logger LOG = LoggerFactory.getLogger(RosteredPlayersController.class);

    private static final int WEEK = 13;

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

    private Set<Integer> unknownPlayers = new HashSet<>();

    int teamsInPlay = 0;

    @SuppressWarnings("unchecked")
    @RequestMapping("/positionCount")
    public final String positionCount(final Map<String, Object> model, final HttpSession session) {

        teamsInPlay = 0;

        final Map<Position, Integer> positionCountMap = new HashMap<>();

        for (League league : propertyBasedLeagues) {
            LOG.debug("Loading week {} for {}", WEEK, league.getName());
            mapPositionCountForLeague(positionCountMap, league);
        }

        LOG.warn("Unknown players is : {}", unknownPlayers.toString().replace(" ", ""));

        model.put("teams", (double) teamsInPlay);
        model.put("leagueCount", (double) propertyBasedLeagues.size());
        model.put("positionScoreMap", positionCountMap);
        return "positionStats";
    }


    private void mapPositionCountForLeague(final Map<Position, Integer> positionCountMap, final League league) {

        final WeeklyResultsWrapper weeklyResultsWrapper = getWeeklyResultsFromFile(league.getName(), WEEK);

        if (weeklyResultsWrapper != null && weeklyResultsWrapper.getMatchupResults() != null) {

            for (MatchupResults matchupResults : weeklyResultsWrapper.getMatchupResults()) {
                loadMatchupResults(positionCountMap, matchupResults);
            }
        }
    }

    private void loadMatchupResults(Map<Position, Integer> positionCountMap, final MatchupResults matchupResults) {

        if (matchupResults != null && matchupResults.getTeams() != null) {

            for (TeamResultDetails teamResultDetails : matchupResults.getTeams()) {
                loadTeamMatchupResults(positionCountMap, teamResultDetails);
            }
        }
    }

    private void loadTeamMatchupResults(Map<Position, Integer> positionCountMap, final TeamResultDetails teamResultDetails) {

        if (teamResultDetails != null) {
        //if (teamResultDetails != null && teamResultDetails.getPlayerResults().size() == 53) {

            teamsInPlay++;

            for(final PlayerResultDetails playerResultDetails : teamResultDetails.getPlayerResults()) {

                if (playerResultDetails != null) {

                    Optional<Player> player = Iterables.tryFind(players, new Predicate<Player>() {

                        public boolean apply(Player testPlayer) {
                            return testPlayer != null && testPlayer.getId() == playerResultDetails.getPlayerId();
                        }
                    });

                    if (!player.isPresent()) {
                        LOG.warn("Count not find player in database for ID {}", playerResultDetails.getPlayerId());
                        unknownPlayers.add(playerResultDetails.getPlayerId());
                    }
                    else {
                        LOG.trace("Incrementing {} count for {}", player.get().getPosition(), player.get().getName());
                        incrementPositionCountInMap(positionCountMap, player.get().getPosition());
                    }
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


    private void incrementPositionCountInMap(Map<Position, Integer> positionCountMap, Position position) {

        if (positionCountMap == null) {
            positionCountMap = new HashMap<>();
        }

        Position mappedPosition = position;
        if (position == Position.CORNERBACK || position == Position.SAFETY) {
            mappedPosition = Position.DEFENSIVE_BACK;
        }

        if (position == Position.DEFENSIVE_TACKLE || position == Position.DEFENSIVE_END) {
            mappedPosition = Position.DEFENSIVE_LINEMAN;
        }

        if (positionCountMap.containsKey(mappedPosition)) {
            positionCountMap.put(mappedPosition, positionCountMap.get(mappedPosition) + 1);

        } else {
            positionCountMap.put(mappedPosition, 1);
        }
    }
}
