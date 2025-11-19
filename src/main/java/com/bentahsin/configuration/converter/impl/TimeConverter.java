package com.bentahsin.configuration.converter.impl;

import com.bentahsin.configuration.converter.Converter;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class TimeConverter implements Converter<String, Long> {

    /**
     * Config'den gelen String süreyi (örn: "1h 10m") saniyeye (Long) çevirir.
     */
    @Override
    public Long convertToField(String source) {
        if (source == null || source.isEmpty()) return 0L;
        return parseTime(source);
    }

    /**
     * Sınıftaki saniye değerini (Long), Config için okunabilir formata (örn: "1h 5m 30s") çevirir.
     */
    @Override
    public String convertToConfig(Long source) {
        if (source == null || source == 0) return "0s";

        long seconds = source;
        long days = TimeUnit.SECONDS.toDays(seconds);
        seconds -= TimeUnit.DAYS.toSeconds(days);

        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);

        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    /**
     * Zaman formatını ayrıştıran gelişmiş parser.
     * Desteklenen formatlar: 1d, 10h, 30m, 45s ve bunların kombinasyonları.
     * Örn: "1h 30m", "100s", "5m"
     */
    private long parseTime(String timeStr) {
        long totalSeconds = 0;
        StringBuilder numberBuffer = new StringBuilder();

        for (char c : timeStr.toLowerCase().toCharArray()) {
            if (Character.isWhitespace(c)) continue;

            if (Character.isDigit(c)) {
                numberBuffer.append(c);
            } else if (Character.isLetter(c)) {
                if (numberBuffer.length() == 0) continue;

                try {
                    long val = Long.parseLong(numberBuffer.toString());
                    switch (c) {
                        case 'd':
                            totalSeconds += val * 86400;
                            break;
                        case 'h':
                            totalSeconds += val * 3600;
                            break;
                        case 'm':
                            totalSeconds += val * 60;
                            break;
                        case 's':
                            totalSeconds += val;
                            break;
                    }
                } catch (NumberFormatException ignored) {}
                numberBuffer.setLength(0);
            }
        }

        if (numberBuffer.length() > 0) {
            try {
                totalSeconds += Long.parseLong(numberBuffer.toString());
            } catch (NumberFormatException ignored) {}
        }

        return totalSeconds;
    }
}