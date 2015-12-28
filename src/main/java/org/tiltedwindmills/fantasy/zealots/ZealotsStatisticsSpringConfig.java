package org.tiltedwindmills.fantasy.zealots;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.tiltedwindmills.fantasy.mfl.model.League;
import org.tiltedwindmills.fantasy.mfl.model.Player;
import org.tiltedwindmills.fantasy.mfl.model.players.PlayerResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Spring configuration file.
 *
 * @author John Daniel
 */
@Configuration
public class ZealotsStatisticsSpringConfig {

    private static final String LEAGUE_PROPERTY_PREFIX = "leagues.";
    private static final String SERVER_ID_PROPERTY_SUFFIX = ".serverid";
    private static final String FRANCHISE_ID_PROPERTY_SUFFIX = ".franchiseId";
    private static final String LEAGUE_ID_PROPERTY_SUFFIX = ".leagueid";
    private static final String LEAGUE_NAME_PROPERTY_SUFFIX = ".name";

    private static final Logger LOG = LoggerFactory.getLogger(ZealotsStatisticsSpringConfig.class);

    /**
     * Initializes the {@code League} from MFL by querying the Developer's API.
     *
     * @return a {@code List} of {@code League}s.
     */
    // CHECKSTYLE:OFF
    // jd - beans cannot be final
    @Bean
    public List<League> propertyBasedLeagues(
                @Value("#{'${leagues}'.split(',')}") final List<String> leagues,
                final Environment environment) {
    // CHECKSTYLE:ON

        final List<League> propertyLeagues = new ArrayList<League>();

        if (leagues != null) {
            for (String leagueKey : leagues) {

                final Integer mflId =
                        environment.getRequiredProperty(LEAGUE_PROPERTY_PREFIX + leagueKey + LEAGUE_ID_PROPERTY_SUFFIX,
                                                        Integer.class);

                final Integer serverId =
                        environment.getRequiredProperty(LEAGUE_PROPERTY_PREFIX + leagueKey + SERVER_ID_PROPERTY_SUFFIX,
                                                        Integer.class);

                final String leagueName =
                        environment.getRequiredProperty(
                                LEAGUE_PROPERTY_PREFIX + leagueKey + LEAGUE_NAME_PROPERTY_SUFFIX);

                final String franchiseId =
                        environment.getProperty(
                                LEAGUE_PROPERTY_PREFIX + leagueKey + FRANCHISE_ID_PROPERTY_SUFFIX);

                // construct the league based on the discovered properties.
                final League league = new League();
                //league.setName(leagueName);
                league.setName(leagueKey);
                league.setId(mflId);
                league.setServerId(serverId);
                league.setFranchiseId(franchiseId);

                // add the skeleton League to our bean.
                propertyLeagues.add(league);

                LOG.debug("Found configured league '{}', using ({}, {}, {})", leagueKey, mflId, serverId, franchiseId);
            }
        }

        LOG.info("Found {} configured leagues", propertyLeagues.size());
        return propertyLeagues;
    }


    /**
     * Initializes the {@code Player} list.
     *
     * @return a {@code List} of {@code Player}s.
     */
    // CHECKSTYLE:OFF
    // jd - beans cannot be final
    @Bean
    public List<Player> players() {
    // CHECKSTYLE:ON

        /* TODO : Currently retrieves from a local file to prevent having to call the service during development.
                Should think of a better approach long term.  May want to keep this impl as an option
                depending on env. */

        PlayerResponse playerResponse = null;
        try {
            final Resource resource = new ClassPathResource("data/mfl_players.json");
            final InputStream resourceInputStream = resource.getInputStream();

            final ObjectMapper mapper = new ObjectMapper();
            playerResponse = mapper.readValue(resourceInputStream, PlayerResponse.class);


        } catch (IOException e) {
            LOG.error("Failed to load MFL players from file: {}", e.getMessage());
        }

        if (playerResponse != null &&
            playerResponse.getWrapper() != null &&
            !CollectionUtils.isEmpty(playerResponse.getWrapper().getPlayers())) {

            LOG.debug("Found {} players", playerResponse.getWrapper().getPlayers().size());
            return playerResponse.getWrapper().getPlayers();
        }

        LOG.debug("No players found.");
        return new ArrayList<Player>();
    }
}
