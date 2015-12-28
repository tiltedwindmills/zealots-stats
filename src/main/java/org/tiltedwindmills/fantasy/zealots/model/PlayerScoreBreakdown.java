package org.tiltedwindmills.fantasy.zealots.model;

import org.springframework.beans.BeanUtils;
import org.tiltedwindmills.fantasy.mfl.model.Player;

public final class PlayerScoreBreakdown extends Player {

    private static final long serialVersionUID = 7144247203994035108L;

    private int top12Finishes;
    private int top24Finishes;
    private int top36Finishes;

    public PlayerScoreBreakdown(final Player player) {

        BeanUtils.copyProperties(player, this);
    }

    public int getTop12Finishes() {
        return top12Finishes;
    }

    public void setTop12Finishes(int top12Finishes) {
        this.top12Finishes = top12Finishes;
    }

    public int getTop24Finishes() {
        return top24Finishes;
    }

    public void setTop24Finishes(int top24Finishes) {
        this.top24Finishes = top24Finishes;
    }

    public int getTop36Finishes() {
        return top36Finishes;
    }

    public void setTop36Finishes(int top36Finishes) {
        this.top36Finishes = top36Finishes;
    }

    public void addTopFinish(int finishingPosition) {

        if (finishingPosition > 0 && finishingPosition <= 12) {
            this.top12Finishes++;

        } else if (finishingPosition > 12 && finishingPosition <= 24) {
            this.top24Finishes++;

        } else if (finishingPosition > 24 && finishingPosition <= 36) {
            this.top36Finishes++;

        }

        // don't do anything if it doesn't match an above conditional.
    }
}
