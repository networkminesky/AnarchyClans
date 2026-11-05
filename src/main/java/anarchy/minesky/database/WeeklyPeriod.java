package anarchy.minesky.database;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

public final class WeeklyPeriod {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private WeeklyPeriod() {
    }

    /** Domingo (início da semana) no fuso configurado, formato ISO (yyyy-MM-dd). */
    public static String currentKey() {
        LocalDate sunday = LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        return sunday.toString();
    }

    public static ZoneId zone() {
        return ZONE;
    }
}
