package com.github.noonmaru.realtime;

import org.bukkit.World;

import java.util.Date;

/**
 * @author Nemo
 */
public class TimeAdapter
{
    private final Date from;

    private final Date to;

    private final int tickOffset;

    private final int tickDuration;

    public TimeAdapter(Date from, Date to, Type type)
    {
        this.from = from;
        this.to = to;
        this.tickOffset = type.offset;
        this.tickDuration = type.period;
    }

    public boolean isValid()
    {
        long time = System.currentTimeMillis();

        return from.getTime() <= time && time < to.getTime();
    }

    public Date getFrom()
    {
        return from;
    }

    public Date getTo()
    {
        return to;
    }

    public int getTickOffset()
    {
        return tickOffset;
    }

    public int getTickDuration()
    {
        return tickDuration;
    }

    public long getCurrentTick()
    {
        long from = this.from.getTime();
        long period = to.getTime() - from;
        long time = System.currentTimeMillis();

        long current = time - from;

        long tick = tickDuration * current / period;

        return tickOffset + tick;
    }

    public enum Type
    {
        DAY(22835, 14315),
        NIGHT(37150, 9685);

        final int offset;

        final int period;

        Type(int offset, int period)
        {
            this.offset = offset;
            this.period = period;
        }
    }
}
