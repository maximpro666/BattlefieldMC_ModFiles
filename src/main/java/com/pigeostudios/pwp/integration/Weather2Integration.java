package com.pigeostudios.pwp.integration;

import com.pigeostudios.pwp.PWP;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Weather2Integration {
    private static Weather2Integration INSTANCE;
    private boolean modChecked;
    private boolean modPresent;

    private Object weatherManager;
    private Class<?> classWeatherAPI;
    private Class<?> classWeatherManager;
    private Class<?> classStorm;
    private Method getWeatherManagerForDim;
    private Method getWindSpeed;
    private Method getWindAngle;
    private Method spawnStorm;
    private Method removeStormsAtPos;
    private Method setIntensity;
    private Method setScale;
    private int lastSandstormStormId = -1;
    private int lastStormStormId = -1;

    private static final Map<Class<?>, Map<String, Method>> REFLECT_METHOD_CACHE = new HashMap<>();

    private static Method resolveMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Map<String, Method> classCache = REFLECT_METHOD_CACHE.get(clazz);
        if (classCache != null) {
            Method m = classCache.get(name + "(" + paramsDesc(paramTypes) + ")");
            if (m != null) return m;
            if (classCache.containsKey(name)) return null;
        }
        try {
            Method m = clazz.getMethod(name, paramTypes);
            REFLECT_METHOD_CACHE.computeIfAbsent(clazz, k -> new HashMap<>()).put(name + "(" + paramsDesc(paramTypes) + ")", m);
            return m;
        } catch (NoSuchMethodException e) {
            REFLECT_METHOD_CACHE.computeIfAbsent(clazz, k -> new HashMap<>()).put(name + "(" + paramsDesc(paramTypes) + ")", null);
            return null;
        }
    }

    private static String paramsDesc(Class<?>... paramTypes) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> c : paramTypes) {
            if (sb.length() > 0) sb.append(",");
            sb.append(c.getSimpleName());
        }
        return sb.toString();
    }

    public static Weather2Integration getInstance() {
        if (INSTANCE == null) INSTANCE = new Weather2Integration();
        return INSTANCE;
    }

    public boolean isModPresent() {
        if (!modChecked) {
            try {
                Class.forName("weather2.WeatherAPI");
                modPresent = true;
                initReflection();
            } catch (ClassNotFoundException e) {
                modPresent = false;
            }
            modChecked = true;
        }
        return modPresent;
    }

    private void initReflection() {
        try {
            classWeatherAPI = Class.forName("weather2.WeatherAPI");
            classWeatherManager = Class.forName("weather2.weathersystem.WeatherManager");
            classStorm = Class.forName("weather2.weathersystem.storm.Storm");
        } catch (ClassNotFoundException e) {
            modPresent = false;
        }
    }

    private void ensureWeatherManager(ServerLevel level) {
        if (!isModPresent() || level == null) return;
        if (weatherManager != null) return;

        try {
            Class<?> clazz = classWeatherAPI;
            Method m = resolveMethod(clazz, "getWeatherManagerForDim", Level.class);
            if (m == null) m = resolveMethod(clazz, "getWeatherManagerForDim", int.class);
            if (m == null) return;

            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Level.class) {
                weatherManager = m.invoke(null, level);
            } else if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == int.class) {
                int dimId = level.dimension().location().hashCode();
                weatherManager = m.invoke(null, dimId);
            }
        } catch (Exception e) {
            PWP.LOGGER.warn("Weather2Integration: failed to get WeatherManager: {}", e.getMessage());
        }
    }

    public void startSandstorm(ServerLevel level, int durationTicks) {
        if (!isModPresent() || level == null) return;
        ensureWeatherManager(level);
        if (weatherManager == null) return;

        try {
            removeExistingSandstorm();
            Method spawn = resolveMethod(classWeatherManager, "spawnStorm", double.class, double.class, double.class, int.class);
            if (spawn == null) return;

            int x = (int) level.getLevelData().getXSpawn();
            int z = (int) level.getLevelData().getZSpawn();
            Object storm = spawn.invoke(weatherManager, (double) x, 90.0, (double) z, 1);
            if (storm != null) {
                lastSandstormStormId = getStormId(storm);
                setStormIntensity(storm, 0.6f);
            }
        } catch (Exception e) {
            PWP.LOGGER.warn("Weather2Integration: failed to start sandstorm: {}", e.getMessage());
        }
    }

    public void stopSandstorm(ServerLevel level) {
        removeExistingSandstorm();
    }

    private void removeExistingSandstorm() {
        if (lastSandstormStormId < 0 || weatherManager == null) return;
        try {
            Method remove = resolveMethod(classWeatherManager, "removeStormsAtPos", int.class, int.class, int.class, double.class);
            if (remove == null) return;
            remove.invoke(weatherManager, 0, 0, 0, 99999.0);
        } catch (Exception ignored) {}
        lastSandstormStormId = -1;
    }

    public void startStorm(ServerLevel level, int durationTicks) {
        if (!isModPresent() || level == null) return;
        ensureWeatherManager(level);
        if (weatherManager == null) return;

        try {
            removeExistingStorm();
            Method spawn = resolveMethod(classWeatherManager, "spawnStorm", double.class, double.class, double.class, int.class);
            if (spawn == null) return;

            int x = (int) level.getLevelData().getXSpawn();
            int z = (int) level.getLevelData().getZSpawn();
            Object storm = spawn.invoke(weatherManager, (double) x, 90.0, (double) z, 0);
            if (storm != null) {
                lastStormStormId = getStormId(storm);
                setStormIntensity(storm, 0.8f);
            }
        } catch (Exception e) {
            PWP.LOGGER.warn("Weather2Integration: failed to start storm: {}", e.getMessage());
        }
    }

    public void stopStorm(ServerLevel level) {
        removeExistingStorm();
    }

    private void removeExistingStorm() {
        if (lastStormStormId < 0 || weatherManager == null) return;
        try {
            Method remove = resolveMethod(classWeatherManager, "removeStormsAtPos", int.class, int.class, int.class, double.class);
            if (remove == null) return;
            remove.invoke(weatherManager, 0, 0, 0, 99999.0);
        } catch (Exception ignored) {}
        lastStormStormId = -1;
    }

    private int getStormId(Object storm) {
        try {
            Method m = resolveMethod(classStorm, "getStormID");
            if (m == null) return -1;
            return (int) m.invoke(storm);
        } catch (Exception e) {
            return -1;
        }
    }

    private void setStormIntensity(Object storm, float intensity) {
        try {
            Field f = classStorm.getField("intensity");
            f.set(storm, intensity);
        } catch (Exception ignored) {}
    }

    public float getWindSpeed(ServerLevel level) {
        if (!isModPresent() || level == null) return 0f;
        ensureWeatherManager(level);
        if (weatherManager == null) return 0f;

        try {
            if (getWindSpeed == null) {
                getWindSpeed = resolveMethod(classWeatherManager, "getWindSpeed");
                if (getWindSpeed == null) return 0f;
            }
            return (float) getWindSpeed.invoke(weatherManager);
        } catch (Exception e) {
            return 0f;
        }
    }

    public float getWindAngle(ServerLevel level) {
        if (!isModPresent() || level == null) return 0f;
        ensureWeatherManager(level);
        if (weatherManager == null) return 0f;

        try {
            if (getWindAngle == null) {
                getWindAngle = resolveMethod(classWeatherManager, "getWindAngle");
                if (getWindAngle == null) return 0f;
            }
            return (float) getWindAngle.invoke(weatherManager);
        } catch (Exception e) {
            return 0f;
        }
    }

    public void reset() {
        weatherManager = null;
        lastSandstormStormId = -1;
        lastStormStormId = -1;
    }
}
