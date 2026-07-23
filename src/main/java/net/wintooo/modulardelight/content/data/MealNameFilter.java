package net.wintooo.modulardelight.content.data;

import java.util.regex.Pattern;

public record MealNameFilter(String pattern, boolean regex, boolean caseInsensitive) {
    public String apply(String input) {
        int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
        Pattern compiled = regex ? Pattern.compile(pattern, flags) : Pattern.compile(Pattern.quote(pattern), flags);
        return compiled.matcher(input).replaceAll("");
    }
}