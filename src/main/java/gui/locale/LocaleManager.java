package gui.locale;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

public class LocaleManager {
    private final Strings strings;
    private final ObjectProperty<Locale> locale = new SimpleObjectProperty<>(Strings.RU);

    public LocaleManager(Strings strings) {
        this.strings = strings;
    }

    public Locale getLocale() { return locale.get(); }

    public void setLocale(Locale newLocale) {
        if (newLocale == null) return;
        Locale.setDefault(newLocale);
        locale.set(newLocale);
    }

    public ObjectProperty<Locale> localeProperty() {
        return locale;
    }

    public String t(String key) {
        return strings.get(getLocale(), key);
    }

    public String formatNumber(Number number) {
        if (number == null) return "";
        return NumberFormat.getNumberInstance(getLocale()).format(number);
    }

    public String formatPrice(Number number) {
        if (number == null) return "";
        return NumberFormat.getNumberInstance(getLocale()).format(number);
    }

    public String formatDateTime(Date date) {
        if (date == null) return "";
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, getLocale()).format(date);
    }

    public String formatDate(java.time.LocalDate localDate) {
        if (localDate == null) return "";
        Date d = Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        return DateFormat.getDateInstance(DateFormat.MEDIUM, getLocale()).format(d);
    }
}