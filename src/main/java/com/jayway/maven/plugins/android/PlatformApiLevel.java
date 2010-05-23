package com.jayway.maven.plugins.android;

/**
 *
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public enum PlatformApiLevel {
    ONE_ONE("1.1", "2"),
    ONE_FIVE("1.5", "3"),
    ONE_SIX("1.6", "4"),
    TWO_ZERO("2.0", "5"),
    TWO_ZERO_ONE("2.01", "6"),
    TWO_ONE("2.1", "7"),
    TWO_TWO("2.2", "8");

    private String platform;
    private String apilevel;

    private PlatformApiLevel(String platform, String apilevel) {
        this.platform = platform;
        this.apilevel = apilevel;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getApilevel() {
        return apilevel;
    }

    public void setApilevel(String apilevel) {
        this.apilevel = apilevel;
    }

    public static String findPlatform(String apilevel) {
        PlatformApiLevel[] allPlatformApiLevels = PlatformApiLevel.values();
        for (PlatformApiLevel platformApiLevel : allPlatformApiLevels) {
            if (platformApiLevel.getApilevel().equals(apilevel)) return platformApiLevel.getPlatform();
        }
        return null;
    }

    public static String findApiLevel(String platform) {
        PlatformApiLevel[] allPlatformApiLevels = PlatformApiLevel.values();
        for (PlatformApiLevel platformApiLevel : allPlatformApiLevels) {
            if (platformApiLevel.getPlatform().equals(platform)) return platformApiLevel.getApilevel();
        }
        return null;
    }

    public static boolean isValidPlatform(String platform) {
        boolean valid = false;
        PlatformApiLevel[] allPlatformApiLevels = PlatformApiLevel.values();
        for (PlatformApiLevel current : allPlatformApiLevels) {
            if (current.getPlatform().equals(platform)) valid = true;
        }
        return valid;
    }

    public static boolean isValidApiLevel(String apiLevel) {
        boolean valid = false;
        PlatformApiLevel[] allPlatformApiLevels = PlatformApiLevel.values();
        for (PlatformApiLevel current : allPlatformApiLevels) {
            if (current.getApilevel().equals(apiLevel)) valid = true;
        }
        return valid;
    }

}
