package com.society.leagues.client.api.domain;

import com.society.leagues.client.api.domain.division.Division;

import javax.validation.constraints.NotNull;
import java.util.Date;


public class Season extends LeagueObject {

    @NotNull
    Division division;
    @NotNull
    String name;
    @NotNull
    Date startDate;
    Date endDate;
    @NotNull
    Integer rounds;

    public Season(Division division, String name, Date startDate, Integer rounds) {
        this.division = division;
        this.name = name;
        this.startDate = startDate;
        this.rounds = rounds;
    }

    public Season() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Division getDivision() {
        return division;
    }

    public void setDivision(Division division) {
        this.division = division;
    }
    
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Integer getRounds() {
        return rounds;
    }

    public void setRounds(Integer rounds) {
        this.rounds = rounds;
    }
}
