package org.grails.plugin.vaadin.services

import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Service used by VaadinTagLib for populating selects with default data. 
 * 
 * @author Francis McKenzie
 */
class VaadinTagDataService {
    protected Date lastTimeZonesRefresh
    protected List<TimeZone> timeZones
    protected List<Locale> locales
    protected List<Currency> currencies
    protected Map<Currency,Locale> currencyLocales = [:]
    
    List<TimeZone> getDefaultTimeZones() {
        if (!timeZones || new Date().time - lastTimeZonesRefresh.time > (1000*60*60)) {
            timeZones = createDefaultTimeZones()
            lastTimeZonesRefresh = new Date()
        }
        return timeZones
    }
    
    List<TimeZone> createDefaultTimeZones() {
        Date date = new Date()
        Map timeZones = [:]
        TimeZone.availableIDs.collect { TimeZone.getTimeZone(it) }.each {
            String shortName = it.getDisplayName(it.inDaylightTime(date), TimeZone.SHORT)
            // Exclude GMT+X timezones
            if (shortName.indexOf('+') < 0 && shortName.indexOf('-') < 0) {
                timeZones."${shortName}${it.rawOffset}" = [timeZone:it, shortName:shortName]
            }
        }
        def result = new ArrayList<TimeZone>(timeZones.values())
        result.sort{a,b-> a.timeZone.rawOffset == b.timeZone.rawOffset ?
            a.shortName.compareTo(b.shortName) :
            (a.timeZone.rawOffset > b.timeZone.rawOffset ? 1 : -1)}
        return result.collect { it.timeZone } 
    }
    
    List<Locale> getDefaultLocales() {
        if (!locales) {
            locales = createDefaultLocales()
        }
        return locales
    }
    
    List<Locale> createDefaultLocales() {
        List<Locale> result = Locale.availableLocales
        result.sort {a,b-> a.displayName.compareTo(b.displayName)}
        result = result.findAll { ! it.country } + result.findAll { it.country }
        return result
    }

    List<Currency> getDefaultCurrencies() {
        if (!currencies) {
            currencies = createDefaultCurrencies()
        }
        return currencies
    }

    List<Currency> createDefaultCurrencies() {
        List<Locale> locales = getDefaultLocales()
        Set<Currency> currencies = new HashSet<Currency>()
        locales.findAll { it.country }.each {
            def currency = Currency.getInstance(it)
            if (currency) {
                currencyLocales[currency] = it
                currencies << currency
            }
        }
        List<Currency> result = new ArrayList<Currency>(currencies)
        result.sort { a,b-> a.currencyCode.compareTo(b.currencyCode) }
        return result
    }
    
    String getSymbol(Currency currency) {
        if (!currency) return ""
        
        // Ensure we've got the map populated
        getDefaultCurrencies()
        
        def locale = currencyLocales[currency]
        return locale ? currency.getSymbol(locale) : null
    }
}
