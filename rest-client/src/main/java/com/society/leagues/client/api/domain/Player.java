package com.society.leagues.client.api.domain;

import javax.validation.constraints.NotNull;

public class Player extends LeagueObject {
    @NotNull
    Season season;
    @NotNull
    User user;
    @NotNull
    Team team;

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
