package com.nyonyix.lithicclaims.data;

public enum Stance
{
    PEACEFUL(0),
    NEUTRAL(1),
    HOSTILE(2),
    INVALID(-1);

    private final int aggression;

    Stance(int aggression)
    {
        this.aggression = aggression;
    }

    public int aggression()
    {
        return aggression;
    }

    public boolean isKnown()
    {
        return this != INVALID;
    }

    public boolean escalatesTo(Stance incoming)
    {
        return isKnown() && incoming.isKnown() && incoming.aggression() > this.aggression ;
    }

    public boolean deescalatesTo(Stance incoming)
    {
        return isKnown() && incoming.isKnown() && incoming.aggression() < this.aggression;
    }
}
