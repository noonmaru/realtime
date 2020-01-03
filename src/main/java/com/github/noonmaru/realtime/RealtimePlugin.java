package com.github.noonmaru.realtime;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

public final class RealtimePlugin extends JavaPlugin implements Runnable
{

    private double latitude;

    private double longitude;

    private int timezone;

    @Override
    public void onEnable()
    {
        saveDefaultConfig();

        getServer().getScheduler().runTaskTimer(this, this, 0, 1);

        this.configFile = new File(getDataFolder(), "config.yml");
        this.lastModified = configFile.lastModified();

        load(getConfig());
        updateTimeAdapter();
    }

    private TimeAdapter timeAdapter;

    private void updateTimeAdapter()
    {
        Calendar calendar = Calendar.getInstance();
        long time = calendar.getTimeInMillis();
        Date sunrise = SunSet.getSunriseTime(calendar, latitude, longitude, timezone);

        if (time < sunrise.getTime()) //자정 이후 일출 이전
        {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            Date sunset = SunSet.getSunsetTime(calendar, latitude, longitude, timezone);

            this.timeAdapter = new TimeAdapter(sunset, sunrise, TimeAdapter.Type.NIGHT);

            getLogger().info("자정 이후 일출 이전 (새벽) " + timeAdapter.getFrom() + " " + timeAdapter.getTo());
            return;
        }

        Date sunset = SunSet.getSunsetTime(calendar, latitude, longitude, timezone);

        if (time < sunset.getTime()) //해돋이 이후 일몰 이전
        {
            this.timeAdapter = new TimeAdapter(sunrise, sunset, TimeAdapter.Type.DAY);

            getLogger().info("일출 이후 일몰 이전 (낮)" + timeAdapter.getFrom() + " " + timeAdapter.getTo());
            return;
        }

        //일몰 이후
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        sunrise = SunSet.getSunriseTime(calendar, latitude, longitude, timezone);
        this.timeAdapter = new TimeAdapter(sunset, sunrise, TimeAdapter.Type.NIGHT);
        getLogger().info("일몰 이후 일출 이전 (밤)" + timeAdapter.getFrom() + " " + timeAdapter.getTo());
    }

    private long lastTick;

    //config reloader
    private long lastModified;

    private File configFile;

    @Override
    public void run()
    {
        long lastModified = configFile.lastModified();

        if (this.lastModified != lastModified)
        {
            this.lastModified = lastModified;

            if (configFile.exists())
            {
                load(YamlConfiguration.loadConfiguration(configFile));
            }
        }

        if (!this.timeAdapter.isValid())
            updateTimeAdapter();

        long tick = timeAdapter.getCurrentTick();

        if (lastTick != tick)
        {
            this.lastTick = tick;

            for (World world : Bukkit.getWorlds())
            {
                world.setTime(tick);
            }
        }
    }

    private void load(ConfigurationSection config)
    {
        getLogger().info("Config reloaded!");
        latitude = config.getDouble("latitude");
        longitude = config.getDouble("longitude");
        timezone = config.getInt("timezone");
    }
}
