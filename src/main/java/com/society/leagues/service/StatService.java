package com.society.leagues.service;

import com.society.leagues.client.api.domain.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
public class StatService {
    final static Logger logger = Logger.getLogger(StatService.class);
    final AtomicReference<List<Stat>> lifetimeDivisionStats = new AtomicReference<>(new CopyOnWriteArrayList<>());
    final AtomicReference<List<Stat>> userStats = new AtomicReference<>(new CopyOnWriteArrayList<>());
    public final AtomicReference<List<Stat>>  handicapStats = new AtomicReference<>(new ArrayList<>(5000));
    final AtomicReference<Map<Season,List<Stat>>> userSeasonStat = new AtomicReference<>(new HashMap<>());
    final AtomicReference<Map<Season,List<Stat>>> userSeasonStatPast = new AtomicReference<>(new HashMap<>());
    @Autowired ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired LeagueService leagueService;
    @Autowired ResultService resultService;

    boolean enableRefresh = true;

    @PostConstruct
    public void init() {
        refresh();
        refreshHandicapStats();
    }

    public List<Stat> getLifetimeDivisionStats() {
        return lifetimeDivisionStats.get();
    }

    public Map<Season,List<Stat>>  getUserSeasonStats() {
        Map<Season,List<Stat>> stats = new HashMap<>();
        stats.putAll(this.userSeasonStat.get());
        stats.putAll(this.userSeasonStatPast.get());
        return stats;
    }

    @Scheduled(fixedRate = 1000*60*5, initialDelay = 1000*60*11)
    public void refresh() {
        if (!enableRefresh)
            return;

        logger.info("Refreshing stats");
        long start = System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
        logger.info("UserSeasonStats  " + (System.currentTimeMillis() - startTime));
        //resultService.refresh();
        logger.info("Done Refreshing stats  (" + (System.currentTimeMillis() - start) + "ms)");
        userStats.get().clear();
        List<Stat> userStats = new ArrayList<>();
        logger.info("Getting user stats");
        for (Season season : leagueService.findAll(Season.class).stream().filter(s->!s.isActive()).collect(Collectors.toList())) {
            userStats.addAll(getSeasonStats(season,false));
        }
        logger.info("Done Getting user stats");
        this.userStats.lazySet(userStats);
    }

    public List<Stat> getSeasonStats(final Season season) {
        return getSeasonStats(season,true);
    }

    public List<Stat> getSeasonStats(final Season season, boolean cache) {
        List<User> users = leagueService.findAll(User.class).stream().filter(User::isReal).filter(u->u.hasSeason(season)).collect(Collectors.toList());
        List<Stat> stats = new ArrayList<>(100);
        for (User user : users) {
            stats.addAll(getUserStats(user,season, cache));
        }
        return stats;
    }

    public List<Stat> getUserStats(User user, Season season) {
        return getUserStats(user,season,true);
    }

    public List<Stat> getUserStats(User user, Season season, boolean cache) {
        List<Stat> stats = new ArrayList<>();
        if (!season.isActive() && cache) {
            stats.addAll(userStats.get().stream()
                    .filter(s->s.getSeason() != null)
                    .filter(s->s.getSeason().equals(season))
                    .filter(s->user.equals(s.getUser())
            ).collect(Collectors.toList()));
            stats.addAll(this.handicapStats.get().stream()
                    .filter(st->st.getUser().equals(user))
                    .filter(st->st.getSeason().equals(season))
                    .collect(Collectors.toList()));
            return stats;
        }
        List<PlayerResult> results = leagueService.findAll(PlayerResult.class).stream().parallel()
                .filter(pr->pr.hasUser(user))
                .filter(pr -> pr.getSeason().equals(season))
                .collect(Collectors.toList());
        List<Team> teams = leagueService.findAll(Team.class).stream().filter(t->t.getSeason().equals(season)).collect(Collectors.toList());
        if (season.isScramble())  {
            stats.add(buildSeasonStats(user,teams,season,
                    results.stream()
                            .filter(pr->pr.getTeamMatch().getDivision() == Division.MIXED_EIGHT)
                            .filter(pr->!pr.isScotch())
                            .collect(Collectors.toList()),
                    StatType.MIXED_EIGHT));
            stats.add(buildSeasonStats(user,teams,season,
                    results.stream()
                            .filter(pr->pr.getTeamMatch().getDivision() == Division.MIXED_NINE)
                            .filter(pr->!pr.isScotch())
                            .collect(Collectors.toList()),
                    StatType.MIXED_NINE));
            stats.add(buildSeasonStats(user,teams,season,
                    results.stream().filter(PlayerResult::isScotch).collect(Collectors.toList()),
                    StatType.MIXED_SCOTCH));
            stats.add(buildSeasonStats(user,teams,season, results, StatType.USER_SEASON));
        } else {
            stats.add(buildSeasonStats(user,teams,season,results,null));
        }


        refreshMatchPoints(user,stats.stream().filter(s->s.getType() == StatType.USER_SEASON).filter(s->s.getSeason().equals(season)).findAny().get(),season);
        stats.addAll(this.handicapStats.get().stream()
                .filter(st->st.getUser().equals(user))
                .filter(st->st.getSeason().equals(season))
                .collect(Collectors.toList()));
        return stats;
    }

    private Stat buildSeasonStats(User user, List<Team> teams, Season season, List<PlayerResult> results, StatType statType) {
        Stat s = Stat.buildPlayerSeasonStats(user,
                season,
                results
        );
        s.setSeason(season);
        s.setTeam(teams.stream().filter(t->t.hasUser(user)).findFirst().orElse(null));
        s.setHandicap(user.getHandicap(season));
        s.setType(statType);

        return s;
    }

    private Stat buildHandicapStats(User user,  Season season, List<PlayerResult> results, StatType statType,Handicap hc) {
        Stat s = Stat.buildPlayerSeasonStats(user,
                season,
                results
        );
        s.setSeason(season);
        s.setHandicap(user.getHandicap(season));
        s.setType(statType);
         s.setHandicap(hc);
        return s;
    }

    private void refreshMatchPoints(User user, Stat stat, Season season) {
        Set<MatchPoints> points = resultService.matchPoints();
        double totalPoints = 0d;
        List<MatchPoints> pointsList = points.stream().parallel()
                .filter(p-> p.getUser() != null && p.getUser().equals(user))
                .filter(p-> p.getPlayerResult() != null)
                .filter(p-> p.getPlayerResult().getSeason() != null)
                .filter(p-> p.getPlayerResult().getSeason().equals(season))
                .collect(Collectors.toList());
        for (MatchPoints matchPoints : pointsList) {
            if (season.isChallenge())
                totalPoints += matchPoints.getWeightedAvg();
            else
                totalPoints += matchPoints.getPoints();
        }
        stat.setPoints(totalPoints);
    }

    public void refreshTeamStats(Team team) {
        final Set<TeamMatch>  teamMatches = leagueService.findAll(TeamMatch.class).stream()
                .filter(t->t.getHome() != null &&  t.getAway() != null)
                .filter(t->t.getSeason().isActive()).collect(Collectors.toSet());
        Stat stat = StatBuilder.buildTeamStats(team,
                teamMatches.parallelStream()
                        .filter(tm -> tm.hasTeam(team))
                        .filter(TeamMatch::isHasResults)
                        .collect(Collectors.toList())
        );
        if (team.isChallenge()) {
            refreshMatchPoints(team.getChallengeUser(),stat,team.getSeason());
        }
        team.setStats(stat);
    }

    @Scheduled(fixedRate = 1000*60*100, initialDelay = 1000*60*3)
    public void refreshHandicapStats() {
        threadPoolTaskExecutor.submit((Runnable) () -> {
            logger.info("refresh handicap stats");
            List<User> users = leagueService.findAll(User.class).stream().filter(User::isReal).collect(Collectors.toList());
            List<PlayerResult> results = leagueService.findAll(PlayerResult.class);
            List<Stat> hs = new ArrayList<>(4000);
            for (User user : users) {
                for (Season season : user.getSeasons()) {
                    Map<Handicap,List<PlayerResult>> userHandicapResults = results.parallelStream()
                            .filter(r->r.hasUser(user))
                            .filter(r->r.getSeason() != null)
                            .filter(r->r.getSeason().equals(season))
                            .collect(Collectors.groupingBy(r->r.getOpponentHandicap(user)));

                    for (Handicap handicap : userHandicapResults.keySet()) {
                        hs.add(buildHandicapStats(user,season,userHandicapResults.get(handicap),StatType.HANDICAP,handicap));
                    }
                }
            }
            handicapStats.lazySet(hs);
            logger.info("done with handicap");
        });

    }

    public void setEnableRefresh(boolean enableRefresh) {
        this.enableRefresh = enableRefresh;
    }
}
