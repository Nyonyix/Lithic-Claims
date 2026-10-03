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

    public boolean escalatesFrom(Stance previous)
    {
        return isKnown() && previous.isKnown() && aggression > previous.aggression();
    }

    public boolean deescalatesFrom(Stance previous)
    {
        return isKnown() && previous.isKnown() && aggression < previous.aggression();
    }
}
