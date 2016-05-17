package com.github.andyell.cucumber;

import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

public class CucumberOptions {

    public static final CucumberOptions DEFAULT_OPTIONS;

    static {
        DEFAULT_OPTIONS = new CucumberOptions();
        DEFAULT_OPTIONS.dryRun = false;
        DEFAULT_OPTIONS.strict = true;
        DEFAULT_OPTIONS.monochrome = true;
    }

    @SuppressWarnings({"unused"})
    @Parameter(required = false, defaultValue = "false", alias = "dry-run")
    private boolean dryRun;

    @SuppressWarnings({"unused"})
    @Parameter(required = false, defaultValue = "true")
    private boolean strict;

    @SuppressWarnings({"unused"})
    @Parameter(required = false, defaultValue = "false")
    private boolean monochrome;

    @SuppressWarnings("unused")
    @Parameter(required = false)
    private List<String> plugins;

    @SuppressWarnings("unused")
    @Parameter(required = false)
    private List<String> names;

    @SuppressWarnings("unused")
    @Parameter(required = false, alias = "features-path")
    public File featuresPath;

    @SuppressWarnings("unused")
    @Parameter(required = false)
    private Tags tags;

    @SuppressWarnings("unused")
    @Parameter(required = false)
    private List<String> glue;

    public boolean isDryRun() {
        return dryRun;
    }

    public boolean isStrict() {
        return strict;
    }

    public boolean isMonochrome() {
        return monochrome;
    }

    public File getFeaturesPath() {
        return featuresPath;
    }

    public String getFormattedGlue() {
        return formatted(glue);
    }

    public String getFormattedPlugin() {
        return formatted(plugins);
    }

    public String getFormattedName() {
        return formatted(names);
    }

    public String getFormattedTags() {
        return tags != null ? tags.getFormattedTags() : "";
    }

    private String formatted(List<String> list) {
        if (list == null || list.size() == 0) {
            return "";
        } else if (list.size() == 1) {
            return quote(list.get(0));
        } else {
            StringBuilder sb = new StringBuilder(quote(list.get(0)));
            for (int i = 1; i < list.size(); i++) {
                sb.append(',').append(quote(list.get(i)));
            }
            return quote(sb.toString());
        }
    }

    private String quote(String s) {
        return "\"" + s + "\"";
    }

}
