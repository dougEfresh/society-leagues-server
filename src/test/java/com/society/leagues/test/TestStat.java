package com.society.leagues.test;

import com.society.leagues.client.api.domain.*;
import org.apache.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestStat  extends BaseTest {

    private static Logger logger = Logger.getLogger(TestUser.class);

    @Test
    public void aTeamStat() {
        for (Season season: seasonApi.active()) {
            List<Team> teams = statApi.teamSeasonStats(season.getId());
            assertTrue(teams.stream().filter(t->!t.getSeason().equals(season)).count() == 0);
            for (Team team : teams) {
                assertTrue(team.getStats().getMatches() == 0);
            }
        }

        for (User user : userApi.active()) {
            List<Stat> stats = statApi.getUserStatsSummary(user.getId());
            assertTrue(stats.stream().filter(s->s.getUser() == null).count() == 0);
            assertTrue(stats.stream().filter(s->!s.getUser().equals(user)).count() == 0);
        }
    }

    @Test
    public void bTeamStat() {
        for (Season season : seasonApi.active().stream().filter(s->!s.isScramble()).collect(Collectors.toList())) {
            User user = userApi.active().stream().filter(u -> u.hasSeason(season)).findFirst().get();
            Team team = teamApi.seasonTeams(season.getId()).stream().filter(t -> t.hasUser(user)).findFirst().get();
            List<TeamMatch> matches = new ArrayList<>();
            if (season.isChallenge()) {
                for (int i = 0; i<=1 ; i++) {
                    TeamMatch tm = teamMatchApi.add(season.getId(), LocalDate.now().toString());
                    tm.setHome(team);
                    tm.setAway(teamApi.seasonTeams(season.getId()).stream().filter(t -> !t.equals(team)).findAny().get());
                    matches.addAll(teamMatchApi.save(Collections.singletonList(tm)));
                }
            } else {
                seasonApi.schedule(season.getId());
                 matches = teamMatchApi.matchesBySeasonList(season.getId()).stream().filter(t -> t.hasTeam(team)).limit(2).collect(Collectors.toList());
                teamMatchApi.save(matches);
            }
            PlayerResult result = playerResultApi.add(matches.get(0).getId()).get(0);
            if (matches.get(0).getHome().equals(team)) {
                result.setPlayerHome(user);
                result.setHomeRacks(5);
                result.setAwayRacks(1);
            } else {
                result.setPlayerAway(user);
                result.setAwayRacks(5);
                result.setHomeRacks(1);
            }
            playerResultApi.save(Collections.singletonList(result));

            result = playerResultApi.add(matches.get(1).getId()).get(0);
            if (matches.get(1).getHome().equals(team)) {
                result.setPlayerHome(user);
                result.setHomeRacks(1);
                result.setAwayRacks(5);
                if (season.isScramble())
                    result.setPlayerHomePartner(teamApi.members(team.getId()).getMembers().stream().filter(u->!u.equals(user)).findAny().get());
            } else {
                result.setPlayerAway(user);
                result.setAwayRacks(1);
                result.setHomeRacks(5);
                if (season.isScramble())
                    result.setPlayerAwayPartner(teamApi.members(team.getId()).getMembers().stream().filter(u->!u.equals(user)).findAny().get());
            }
            playerResultApi.save(Collections.singletonList(result));

            List<Stat> stats = statApi.getUserSeasonStats(user.getId(),season.getId());
            //assertTrue(stats.stream().filter(s->s.getType() == StatType.ALL).count() == 1);
            assertTrue(stats.stream().filter(s->s.getType() == StatType.USER_SEASON).count() == 1);
            assertTrue(stats.stream().filter(s->s.getType() == StatType.MIXED_EIGHT).count() == 0);
            assertTrue(stats.stream().filter(s->s.getType() == StatType.MIXED_NINE).count() == 0);
            assertTrue(stats.stream().filter(s->s.getType() == StatType.MIXED_SCOTCH).count() == 0);
            for (Stat stat : stats) {
                if (stat.getType() == StatType.USER_SEASON) {
                    assertEquals(new Integer(2),stat.getMatches());
                    assertEquals(new Integer(1),stat.getWins());
                    assertEquals(new Integer(1),stat.getLoses());
                    assertEquals(new Integer(6),stat.getRacksWon());
                    assertEquals(new Integer(6),stat.getRacksLost());
                }

                if (season.isScramble() && stat.getType() == StatType.MIXED_EIGHT) {
                    assertEquals(new Integer(1),stat.getMatches());
                    assertEquals(new Integer(1),stat.getWins());
                    assertEquals(new Integer(5),stat.getRacksWon());
                    assertEquals(new Integer(1),stat.getRacksLost());
                    assertEquals(new Integer(0),stat.getLoses());
                }

                if (season.isScramble() && stat.getType() == StatType.MIXED_NINE) {
                    assertEquals(new Integer(1),stat.getMatches());
                    assertEquals(new Integer(0),stat.getWins());
                    assertEquals(new Integer(1),stat.getRacksWon());
                    assertEquals(new Integer(5),stat.getRacksLost());
                    assertEquals(new Integer(1),stat.getLoses());
                }
            }
        }
    }
    @Test
    public void cTeamStat() {
        Season season = seasonApi.active().stream().filter(s->s.isScramble()).findFirst().get();
        User user = userApi.active().stream().filter(u -> u.hasSeason(season)).findFirst().get();
        Team team = teamApi.seasonTeams(season.getId()).stream().filter(t -> t.hasUser(user)).findFirst().get();
        List<TeamMatch> matches = new ArrayList<>();
        seasonApi.schedule(season.getId());
        matches = teamMatchApi.matchesBySeasonList(season.getId()).stream().filter(t -> t.hasTeam(team)).limit(2).collect(Collectors.toList());
        matches.get(0).setDivision(Division.MIXED_EIGHT);
        matches.get(1).setDivision(Division.MIXED_NINE);
        teamMatchApi.save(matches);

         PlayerResult result = playerResultApi.add(matches.get(0).getId()).get(0);
        if (matches.get(0).getHome().equals(team)) {
            result.setPlayerHome(user);
            result.setHomeRacks(5);
                result.setAwayRacks(1);
        } else {
            result.setPlayerAway(user);
            result.setAwayRacks(5);
            result.setHomeRacks(1);
        }
        playerResultApi.save(Collections.singletonList(result));
        result = playerResultApi.add(matches.get(1).getId()).get(0);
        if (matches.get(1).getHome().equals(team)) {
            result.setPlayerHome(user);
            result.setHomeRacks(1);
            result.setAwayRacks(5);
            result.setPlayerHomePartner(teamApi.members(team.getId()).getMembers().stream().filter(u->!u.equals(user)).findAny().get());
        } else {
            result.setPlayerAway(user);
            result.setAwayRacks(1);
            result.setHomeRacks(5);
            result.setPlayerAwayPartner(teamApi.members(team.getId()).getMembers().stream().filter(u->!u.equals(user)).findAny().get());
        }
        playerResultApi.save(Collections.singletonList(result));
        List<Stat> stats = statApi.getUserSeasonStats(user.getId(),season.getId());
        assertTrue(stats.stream().filter(s->s.getType() == StatType.USER_SEASON).count() == 1);
        assertTrue(stats.stream().filter(s->s.getType() == StatType.MIXED_EIGHT).count() == 1);
        assertTrue(stats.stream().filter(s->s.getType() == StatType.MIXED_NINE).count() == 1);
        assertTrue(stats.stream().filter(s->s.getType() == StatType.MIXED_SCOTCH).count() == 1);
        Stat stat = stats.stream().filter(s->s.getType() == StatType.MIXED_EIGHT).findAny().get();
        assertEquals(new Integer(1),stat.getMatches());
        assertEquals(new Integer(1),stat.getWins());
        assertEquals(new Integer(0),stat.getLoses());
        assertEquals(new Integer(5),stat.getRacksWon());
        assertEquals(new Integer(1),stat.getRacksLost());

        stat = stats.stream().filter(s->s.getType() == StatType.MIXED_NINE).findAny().get();
        assertEquals(new Integer(0),stat.getMatches());
        assertEquals(new Integer(0),stat.getWins());
        assertEquals(new Integer(0),stat.getLoses());
        assertEquals(new Integer(0),stat.getRacksWon());
        assertEquals(new Integer(0),stat.getRacksLost());


        stat = stats.stream().filter(s->s.getType() == StatType.MIXED_SCOTCH).findAny().get();
        assertEquals(new Integer(1),stat.getMatches());
        assertEquals(new Integer(0),stat.getWins());
        assertEquals(new Integer(1),stat.getLoses());
        assertEquals(new Integer(1),stat.getRacksWon());
        assertEquals(new Integer(5),stat.getRacksLost());

        stat = stats.stream().filter(s->s.getType() == StatType.USER_SEASON).findAny().get();
        assertEquals(new Integer(2),stat.getMatches());
        assertEquals(new Integer(1),stat.getWins());
        assertEquals(new Integer(1),stat.getLoses());
        assertEquals(new Integer(6),stat.getRacksWon());
        assertEquals(new Integer(6),stat.getRacksLost());

    }
}
