package com.cx.plugin.web.resolver;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * Created by caixiang on 2017/9/26.
 */
@Component("localeResolver")
public class HeliosLocaleResolver extends AcceptHeaderLocaleResolver {

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        System.out.println("my locale resolver start");
        //todo
        return super.resolveLocale(request);
//        return Locale.CHINA;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException(
                "Cannot change helios locale rules - use a different locale resolution strategy");
    }
}
