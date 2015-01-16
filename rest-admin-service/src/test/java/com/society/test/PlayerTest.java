package com.society.test;

import com.society.leagues.Main;
import com.society.leagues.client.ApiFactory;
import com.society.leagues.client.api.admin.*;
import com.society.leagues.client.api.Role;
import com.society.leagues.client.api.domain.*;
import com.society.leagues.client.api.domain.division.Division;
import com.society.leagues.client.api.domain.division.DivisionType;
import com.society.leagues.client.api.domain.league.League;
import com.society.leagues.client.api.domain.league.LeagueType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Main.class})
@IntegrationTest(value = {"server.port:0","daemon:true","debug:true"})
public class PlayerTest extends TestBase {
    PlayerAdminApi api;
    SeasonAdminApi seasonApi;
    LeagueAdminApi leagueApi;
    DivisionAdminApi divisionApi;
    TeamAdminApi teamApi;
    UserAdminApi userApi;

    @Before
    public void setup() throws Exception {
        super.setup();
        String token = authenticate(Role.ADMIN);
        api = ApiFactory.createApi(PlayerAdminApi.class, token, baseURL);
        leagueApi = ApiFactory.createApi(LeagueAdminApi.class, token, baseURL);
        divisionApi = ApiFactory.createApi(DivisionAdminApi.class, token, baseURL);
        seasonApi = ApiFactory.createApi(SeasonAdminApi.class, token, baseURL);
        teamApi = ApiFactory.createApi(TeamAdminApi.class, token, baseURL);
        userApi = ApiFactory.createApi(UserAdminApi.class, token, baseURL);
    }

    @Test
    public void testCreate() {
        League league = new League(LeagueType.INDIVIDUAL);
        league = leagueApi.create(league);
        assertNotNull(league);

        Division division = new Division(DivisionType.EIGHT_BALL_THURSDAYS,league);
        division = divisionApi.create(division);
        assertNotNull(division);

        Season season = new Season(division,"Cool", new Date(),10);
        season = seasonApi.create(season);
        assertNotNull(season);

        Team team = new Team(UUID.randomUUID().toString(),division);
        team  = teamApi.create(team);
        assertNotNull(team);

        User user = new User(UUID.randomUUID().toString(),"password");
        user = userApi.create(user);
        assertNotNull(user);

        Player player = new Player(season,user,team,"D");

        player = api.create(player);
        assertNotNull(player);
        assertNotNull(player.getId());

    }
}
