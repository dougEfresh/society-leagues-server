package com.society.leagues.client.api.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserChallengeGroup {
    User challenger;
    User opponent;
    LocalDate date;
    List<Challenge> challenges = new ArrayList<>();

    public User getChallenger() {
        return challenger;
    }

    public void setChallenger(User challenger) {
        this.challenger = challenger;
    }

    public User getOpponent() {
        return opponent;
    }

    public void setOpponent(User opponent) {
        this.opponent = opponent;
    }

    public List<Challenge> getChallenges() {
        return challenges;
    }

    public void setChallenges(List<Challenge> challenges) {
        this.challenges = challenges;
    }

    public void addChallenge(Challenge challenge) {
        challenges.add(challenge);
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserChallengeGroup)) return false;

        UserChallengeGroup group = (UserChallengeGroup) o;

        if (challenger != null ? !challenger.equals(group.challenger) : group.challenger != null) return false;
        if (opponent != null ? !opponent.equals(group.opponent) : group.opponent != null) return false;
        return !(date != null ? !date.equals(group.date) : group.date != null);

    }

    @Override
    public int hashCode() {
        int result = challenger != null ? challenger.hashCode() : 0;
        result = 31 * result + (opponent != null ? opponent.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        return result;
    }
}