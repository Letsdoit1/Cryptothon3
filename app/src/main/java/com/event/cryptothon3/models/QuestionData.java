package com.event.cryptothon3.models;

public class QuestionData {
    private Integer time;
    private Integer level;
    private String rank;
    private Integer maxRank;
    private String question;
    private String hint;
    private String teamName;
    private String code;
    private Integer ansLength;
    private Boolean isSuccess;
    private Boolean earlyBird;
    private boolean hintVisibility;
    public String getRank() {
        return rank;
    }
    public void setRank(String rank) {
        this.rank = rank;
    }

    public Integer getAnsLength() {
        return ansLength;
    }

    public void setAnsLength(Integer ansLength) {
        this.ansLength = ansLength;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    private String error;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getMaxRank() {
        return maxRank;
    }

    public void setMaxRank(Integer maxRank) {
        this.maxRank = maxRank;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }


    public void sethintVisibility(boolean hintVisibility) {
        this.hintVisibility = hintVisibility;
    }

    public boolean gethintVisibility() {
        return hintVisibility;
    }
    public Boolean getEarlyBird() {
        return earlyBird;
    }
    public void setEarlyBird(Boolean earlyBird) {
        this.earlyBird = earlyBird;
    }
    public Boolean getIsSuccess() {
        return isSuccess;
    }
    public void setIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}
