package com.society.leagues.client.api.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.society.leagues.client.api.domain.converters.DateTimeDeSerializer;
import com.society.leagues.client.api.domain.views.PlayerResultView;
import org.springframework.data.mongodb.core.mapping.DBRef;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class PlayerResult extends LeagueObject {

    @NotNull @DBRef TeamMatch teamMatch;
    @NotNull @DBRef User playerHome;
    @NotNull @DBRef User playerAway;
    @NotNull Integer homeRacks = -1;
    @NotNull Integer awayRacks = -1;
    @NotNull Integer matchNumber = -1;
    @NotNull Handicap playerHomeHandicap;
    @NotNull Handicap playerAwayHandicap;

    Team referenceTeam = null;
    User referenceUser = null;

    public PlayerResult() {
    }

    public PlayerResult(TeamMatch teamMatch, User playerHome, User playerAway, Integer homeRacks, Integer awayRacks,
                        Integer matchNumber, Handicap playerHomeHandicap, Handicap playerAwayHandicap) {
        this.teamMatch = teamMatch;
        this.playerHome = playerHome;
        this.playerAway = playerAway;
        this.homeRacks = homeRacks;
        this.awayRacks = awayRacks;
        this.matchNumber = matchNumber;
        this.playerHomeHandicap = playerHomeHandicap;
        this.playerAwayHandicap = playerAwayHandicap;
    }

    public PlayerResult(String id) {
        this.id = id;
    }

    public TeamMatch getTeamMatch() {
        return teamMatch;
    }

    public void setTeamMatch(TeamMatch teamMatch) {
        this.teamMatch = teamMatch;
    }

    public User getPlayerHome() {
        return playerHome;
    }

    public void setPlayerHome(User playerHome) {
        this.playerHome = playerHome;
    }

    public User getPlayerAway() {
        return playerAway;
    }

    public void setPlayerAway(User playerAway) {
        this.playerAway = playerAway;
    }

    public Integer getHomeRacks() {
        return homeRacks;
    }

    public void setHomeRacks(Integer homeRacks) {
        this.homeRacks = homeRacks;
    }

    public Integer getAwayRacks() {
        return awayRacks;
    }

    public void setAwayRacks(Integer awayRacks) {
        this.awayRacks = awayRacks;
    }

    public Integer getMatchNumber() {
        return matchNumber;
    }

    public void setMatchNumber(Integer matchNumber) {
        this.matchNumber = matchNumber;
    }

    @JsonView(PlayerResultView.class)
    public Season getSeason() {
        return teamMatch.getSeason();
    }

    public Handicap getPlayerHomeHandicap() {
        return playerHomeHandicap;
    }

    public void setPlayerHomeHandicap(Handicap playerHomeHandicap) {
        this.playerHomeHandicap = playerHomeHandicap;
    }

    public Handicap getPlayerAwayHandicap() {
        return playerAwayHandicap;
    }

    public void setPlayerAwayHandicap(Handicap playerAwayHandicap) {
        this.playerAwayHandicap = playerAwayHandicap;
    }

    public User getWinner() {
        if (homeRacks  == null || awayRacks == null)
            return null;

        return homeRacks > awayRacks ? playerHome : playerAway;
    }

    public Integer getWinnerRacks() {
        if (homeRacks  == null ||  awayRacks == null)
            return 0;

        return homeRacks > awayRacks ? homeRacks : awayRacks;
    }

    public String  getWinnerHandicap() {
        if (getWinner() == null)
            return null;
        HandicapSeason hc =  getWinner().getHandicapSeasons().stream().filter(s->s.getSeason().equals(getSeason())).findFirst().orElse(null);
        if (hc == null) {return null;}
        return Handicap.format(hc.getHandicap());
    }

    public User getLoser() {
         if (homeRacks  == null ||  awayRacks == null)
            return null;

        return homeRacks > awayRacks ? playerAway : playerHome;
    }

    public Integer getLoserRacks() {
        if (homeRacks  == null ||  awayRacks == null)
            return 0;

        return homeRacks > awayRacks ? awayRacks : homeRacks;
    }

    public String getLoserHandicap() {
        if (getLoser() == null)
            return null;
        HandicapSeason hc =  getLoser().getHandicapSeasons().stream().filter(s->s.getSeason().equals(getSeason())).findFirst().orElse(null);
        if (hc == null) {return null;}
        return Handicap.format(hc.getHandicap());
    }

    public boolean isWinner(User u) {
        return u != null && u.equals(getWinner());
    }

    @JsonDeserialize(using = DateTimeDeSerializer.class)
    @JsonIgnore
    public LocalDateTime getMatchDate() {
        if (getTeamMatch() == null)
            return null;
        return getTeamMatch().getMatchDate();
    }

    public boolean hasUser(User u) {
        return u!= null && (u.equals(playerHome) || u.equals(playerAway));
    }

    public boolean hasTeam(Team t) {
        return t!= null && (t.equals(getTeamMatch().getHome()) || t.equals(getTeamMatch().getAway()));
    }

    public Handicap getHandicap(User u) {
        if (u.equals(playerAway)) {
            return playerAwayHandicap ;
        }
        if (u.equals(playerHome)) {
            return playerAwayHandicap ;
        }
        return null;
    }

    public boolean isNine() {
        return getSeason().getDivision() == Division.NINE_BALL_TUESDAYS || getSeason().getDivision() == Division.NINE_BALL_CHALLENGE;
    }

    public void setReferenceTeam(Team referenceTeam) {
        this.referenceTeam = referenceTeam;
    }

    public User getOpponent() {
        if (referenceTeam != null)
            return referenceTeam.equals(teamMatch.getHome()) ? playerAway : playerHome;

        if (referenceUser != null)
            return referenceUser.equals(playerHome) ? playerAway : playerHome;


        return null;

    }

    public User getTeamMember() {
        if (referenceTeam != null)
            return referenceTeam.equals(teamMatch.getHome()) ? playerHome : playerAway;

        return referenceUser;
    }

    public boolean isWin() {
        if (referenceTeam != null)
            return referenceTeam.equals(teamMatch.getHome()) ? homeRacks > awayRacks : awayRacks > homeRacks;

        if (referenceUser != null)
            return isWinner(referenceUser);

        return false;
    }

    public String getOpponentHandicap() {
        if (referenceTeam != null)
            return referenceTeam.equals(teamMatch.getHome()) ?  Handicap.format(playerAwayHandicap) : Handicap.format(playerHomeHandicap);

        if (referenceUser != null)
            return referenceUser.equals(playerHome) ? Handicap.format(playerAwayHandicap) : Handicap.format(playerHomeHandicap);

        return null;
    }

    public String getTeamMemberHandicap() {
        if (referenceTeam != null)
            return referenceTeam.equals(teamMatch.getHome()) ?  Handicap.format(playerHomeHandicap) : Handicap.format(playerAwayHandicap);

        if (referenceUser != null)
            return referenceUser.equals(playerHome) ? Handicap.format(playerHomeHandicap) : Handicap.format(playerAwayHandicap);

        return null;
    }

    public Team getOpponentTeam() {
        if (referenceUser != null) {
            return teamMatch.getHome().getMembers().contains(referenceUser) ? teamMatch.getAway() : teamMatch.getHome();
        }

        if (referenceTeam != null)
            return referenceTeam.equals(teamMatch.getHome()) ? teamMatch.getAway() : teamMatch.getHome();

        return null;
    }

        public Team getTeam() {
        if (referenceUser != null) {
            return teamMatch.getHome().getMembers().contains(referenceUser) ? teamMatch.getHome() : teamMatch.getAway();
        }
        return referenceTeam;
    }

    public Integer getTeamMemberRacks() {
        if (referenceUser != null) {
            return referenceUser.equals(playerHome) ?  homeRacks : awayRacks;
        }
        if (referenceTeam != null) {
            return referenceTeam.equals(teamMatch.getHome()) ? homeRacks : awayRacks;
        }

        return 0;
    }


    public Integer getOpponentRacks() {
        if (referenceUser != null) {
            return referenceUser.equals(playerHome) ?  awayRacks : homeRacks;
        }
        if (referenceTeam != null) {
            return referenceTeam.equals(teamMatch.getHome()) ? awayRacks : homeRacks;
        }

        return 0;
    }

    public void setReferenceUser(User referenceUser) {
        this.referenceUser = referenceUser;
    }

    public void clearReference() {
        referenceTeam = null;
        referenceUser = null;
    }

    @Override
    public String toString() {
        return "PlayerResult{" +
                "teamMatch=" + teamMatch +
                ", playerHome=" + playerHome +
                ", playerAway=" + playerAway +
                ", homeRacks=" + homeRacks +
                ", awayRacks=" + awayRacks +
                ", matchNumber=" + matchNumber +
                ", playerHomeHandicap=" + playerHomeHandicap +
                ", playerAwayHandicap=" + playerAwayHandicap +
                '}';
    }
}
