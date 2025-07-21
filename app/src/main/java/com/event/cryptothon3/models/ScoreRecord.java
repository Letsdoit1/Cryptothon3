package com.event.cryptothon3.models;

public class ScoreRecord {
    String rank;
    String teamName;
    String level;
    String score;
    public ScoreRecord(String rank, String teamName, String level, String score) {
        this.rank = rank;
        this.teamName = teamName;
        this.level = level;
        this.score = score;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }
}
