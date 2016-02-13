package com.society.leagues.client.api.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.society.leagues.client.views.PlayerResultSummary;
import com.society.leagues.converters.DateTimeDeSerializer;
import com.society.leagues.converters.DateTimeSerializer;
import org.omg.CORBA.UNKNOWN;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

@SuppressWarnings("unused")
public class TeamMatch extends LeagueObject {

    @JsonView(PlayerResultSummary.class) @NotNull @DBRef Team home;
    @JsonView(PlayerResultSummary.class) @NotNull @DBRef Team away;
    @JsonSerialize(using = DateTimeSerializer.class)
    @JsonDeserialize(using = DateTimeDeSerializer.class)
    @JsonView(PlayerResultSummary.class) @NotNull LocalDateTime matchDate;

    @JsonView(PlayerResultSummary.class) Division division = null;
    @JsonView(PlayerResultSummary.class)  Integer homeRacks = 0;
    @JsonView(PlayerResultSummary.class)  Integer awayRacks = 0;
    @JsonView(PlayerResultSummary.class)  Integer setHomeWins = 0;
    @JsonView(PlayerResultSummary.class)  Integer setAwayWins = 0;
    @JsonView(PlayerResultSummary.class)  Integer matchNumber = 0;
    User referenceUser = null;
    Boolean hasPlayerResults = false;
    Status status = null;
    @JsonView(PlayerResultSummary.class)  Integer forfeits = 0;
    @JsonView(PlayerResultSummary.class)  Integer homeForfeits = 0;
    @JsonView(PlayerResultSummary.class)  Integer awayForfeits = 0;
    Integer handicapRacks = 0;

    @JsonIgnore @Transient String date;
    @JsonIgnore @Transient String time;
    @Transient String race = "";

    public TeamMatch(Team home, Team away, LocalDateTime matchDate) {
        this.home = home;
        this.away = away;
        this.matchDate = matchDate;
    }

    public TeamMatch() {
    }

    public Status getStatus() {
        LocalDateTime now  = LocalDateTime.now();
        if (getMatchDate() != null && getMatchDate().isBefore(now) && !isHasResults()) {
            return Status.PENDING;
        }
        return status;
    }

    public String getGameType() {
        if (getDivision() == null)
            return "Unknown";

        switch (getDivision()) {
            case NINE_BALL_CHALLENGE:
            case NINE_BALL_TUESDAYS:
            case MIXED_NINE:
                return "9";
            case MIXED_EIGHT:
            case EIGHT_BALL_THURSDAYS:
            case EIGHT_BALL_WEDNESDAYS:
                return "8";
        }
        return "N/A";
    }

    public void setDivision(Division division) {
        this.division = division;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public TeamMatch(String id) {
        this.id = id;
    }

    public Team getHome() {
        return home;
    }

    public void setHome(Team home) {
        this.home = home;
    }

    public Team getAway() {
        return away;
    }

    public void setAway(Team away) {
        this.away = away;
    }

    public LocalDateTime getMatchDate() {
        if (matchDate != null)
            return matchDate;

        if (date != null && time != null) {
            try {
                return LocalDateTime.parse(date + "T" + time);
            } catch (Exception e) {

            }
        }
        return null;
    }

    public void setMatchDate(LocalDateTime matchDate) {
        this.matchDate = matchDate;
    }

    public Season getSeason() {
        if (home != null)
            return home.getSeason();

        return null;
    }

    public String score() {
        return String.format("%s-%s", getHomeRacks(), getAwayRacks());
    }

    public Division getDivision() {
        if (division == null && getSeason() != null) {
            return getSeason().getDivision();
        }
        return division;
    }

    public boolean isNine() {
        return getSeason() != null && getSeason().isNine();
    }

    public boolean isChallenge() {
        return getDivision() != null && getDivision().isChallenge();
    }

    public Integer getAwayRacks() {
        return awayRacks;
    }

    public void setAwayRacks(Integer awayRacks) {
        this.awayRacks = awayRacks;
    }

    public Integer getHomeRacks() {
        return homeRacks;
    }

    public void setHomeRacks(Integer homeRacks) {
        this.homeRacks = homeRacks;
    }

    public boolean hasUser(User user) {
        return home.getMembers().getMembers().contains(user) || away.getMembers().getMembers().contains(user);
    }

    public boolean hasTeam(Team team) {
        if (home == null || away == null) {
            return false;
        }
        return  home.equals(team) || away.equals(team);
    }

    public boolean isWinner(Team t) {
        if (t.equals(home) ) {
            return homeRacks > awayRacks;
        }
        return awayRacks > homeRacks;
    }

    public Integer getRacks(Team t) {
        if (t.equals(home) ) {
            return homeRacks;
        }
        return awayRacks;
    }

    public Integer getOpponentRacks(Team t) {
        if (t.equals(home) ) {
            return awayRacks;
        }
        return homeRacks;
    }

    public Integer getSetAwayWins() {
        return setAwayWins;
    }

    public void setSetAwayWins(Integer setAwayWins) {
        this.setAwayWins = setAwayWins;
    }

    public Integer getSetHomeWins() {
        return setHomeWins;
    }

    public void setSetHomeWins(Integer setHomeWins) {
        this.setHomeWins = setHomeWins;
    }

    public boolean isHasResults() {
        if (homeRacks > 0 && homeRacks.equals(homeForfeits))
            return true;
        if (awayRacks > 0 && awayRacks.equals(awayForfeits))
            return true;

        return homeRacks + awayRacks > 0;
    }

    public Integer getSetWins(Team team) {
        return team.equals(home)? setHomeWins : setAwayWins;
    }

    public Integer getSetLoses(Team team) {
        return team.equals(home)? setAwayWins : setHomeWins;
    }

    public Integer getWinnerSetWins() {
        if (!isHasResults())
            return null;
        return homeRacks > awayRacks ? setHomeWins : setAwayWins;
    }

    public Integer getWinnerSetLoses() {
        if (!isHasResults())
            return null;

        return homeRacks > awayRacks ? setAwayWins : setHomeWins;
    }

    public Integer getLoserSetWins() {
        if (!isHasResults())
            return null;
        return homeRacks > awayRacks ? setAwayWins : setHomeWins;
    }

    public Integer getLoserSetLoses() {
        return homeRacks > awayRacks ? setHomeWins : setAwayWins;
    }

    public Integer getSetHomeLost() {
        return setAwayWins;
    }

    public Integer getSetAwayLost() {
        return setHomeWins;
    }

    public Team getOpponentTeam() {
        if (referenceUser == null)
            return null;

        return home.getMembers().getMembers().contains(referenceUser) ? away : home;
    }

    public Integer getWinnerRacks() {
        if (!isHasResults())
            return null;

        return homeRacks > awayRacks ? homeRacks : awayRacks;
    }

    public Integer getLoserRacks() {
        if (!isHasResults())
            return null;

        return homeRacks > awayRacks ? awayRacks : homeRacks;
    }

    public Team getWinner() {
        if (homeRacks.equals(awayRacks))
            return home;

        return homeRacks > awayRacks ? home : away;
    }

    public Team getLoser() {
        if (awayRacks.equals(homeRacks)) {
            return away;
        }
        return awayRacks > homeRacks ? home : away;
    }

    public String getScore() {
        if (!isChallenge()) {
            return null;
        }
        return getWinnerRacks() == null ? null : getWinnerRacks() + "-" + getLoserRacks();
    }

    public String getRace() {
        if (isChallenge()) {
            if (getHome() == null || getHome().getChallengeUser() == null)
                return "";
            if (getAway() == null || getAway().getChallengeUser() == null)
                return "";
            Handicap h  = getHome().getChallengeUser().getHandicap(getSeason());
            Handicap a = getAway().getChallengeUser().getHandicap(getSeason());
            return Handicap.race(h,a);
        }
        if (race != null && !race.isEmpty())
            return race;

        return "";
    }

    public User getChallenger() {
        if (isChallenge()) {
            return getHome().getChallengeUser();
        }
        return null;
    }

    public User getOpponent() {
        if (isChallenge()) {
            return getAway().getChallengeUser();
        }
        return null;
    }

    public boolean isForfeit() {
        return (homeRacks == 0 && awayRacks > 0) || (awayRacks == 0 && homeRacks > 0);
    }

    public Integer getForfeits() {
        return forfeits;
    }

    public void setForfeits(Integer forfeits) {
        this.forfeits = forfeits;
    }

    public Integer getHandicapRacks() {
        return handicapRacks;
    }

    public void setHandicapRacks(Integer handicapRacks) {
        this.handicapRacks = handicapRacks;
    }

    public Integer getHomeForfeits() {
        return homeForfeits;
    }

    public void setHomeForfeits(Integer homeForfeits) {
        this.homeForfeits = homeForfeits;
    }

    public Integer getAwayForfeits() {
        return awayForfeits;
    }

    public void setAwayForfeits(Integer awayForfeits) {
        this.awayForfeits = awayForfeits;
    }

    @JsonIgnore
    public String getTime() {
        return getMatchDate().toLocalTime().toString();
    }

    @JsonIgnore
    public String getDate() {
        return getMatchDate().toLocalDate().toString();
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "{" +
                "home=" + home.getName() +
                ", away=" + away.getName() +
                ", matchDate=" + matchDate.toLocalDate() +
                '}';
    }

    public void setRace(String race) {
        this.race = race;
    }

    public Boolean getHasPlayerResults() {
        return hasPlayerResults;
    }

    public void setHasPlayerResults(Boolean hasPlayerResults) {
        this.hasPlayerResults = hasPlayerResults;
    }

    public Integer getMatchNumber() {
        return matchNumber;
    }

    public void setMatchNumber(Integer matchNumber) {
        this.matchNumber = matchNumber;
    }

    public void setReferenceUser(User referenceUser) {
        this.referenceUser = referenceUser;
    }

    public static Comparator<TeamMatch> sortAcc() {
        return new Comparator<TeamMatch>() {
            @Override
            public int compare(TeamMatch o1, TeamMatch o2) {
                return o1.getMatchDate().compareTo(o2.getMatchDate());
            }
        };
    }

    public static boolean isSameMatch(TeamMatch t1, TeamMatch t2) {
        if (!t2.getMatchDate().equals(t1.getMatchDate())) {
            return false;
        }
        if (t1.getHome().equals(t2.getHome()) && t1.getAway().equals(t2.getAway())) {
            return true;
        }

        return t1.getHome().equals(t2.getAway()) && t1.getAway().equals(t2.getHome());

    }

    public boolean hasBothTeams(Team a, Team b) {
        return hasTeam(a) && hasTeam(b);
    }

}
