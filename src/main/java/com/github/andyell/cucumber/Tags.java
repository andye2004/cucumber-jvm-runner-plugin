package com.github.andyell.cucumber;

import org.apache.maven.plugins.annotations.Parameter;

public class Tags {

    private static final String TILDA = "~";

    @Parameter(required = false)
    private String include;

    @Parameter(required = false)
    private String exclude;

    public String getFormattedTags() {
        if ((include == null || include.length() == 0)
                && (exclude == null || exclude.length() == 0)) {

            return "";
        }

        if (exclude == null || exclude.length() == 0) {
            return format(include, "");
        }

        if (include == null || include.length() == 0) {
            return format(exclude, TILDA);
        }

        String includes = format(include, "");
        String excludes = format(exclude, TILDA);

        return includes + "," + excludes;
    }

    private String format(String tags, String tilda) {
        String[] includes = tags.split(",");

        if (includes.length == 1) {
            return quote(tilda + includes[0].trim());
        } else {
            StringBuilder sb = new StringBuilder(quote(tilda + includes[0].trim()));
            for (int i = 1; i < includes.length; i++) {
                sb.append(',').append(quote(tilda + includes[i].trim()));
            }
            return sb.toString();
        }
    }

    private String quote(String s) {
        return "\"" + s + "\"";
    }

    public static void main(String[] args) {
        Tags tags = new Tags();
        tags.include = "in1  ,  in2   ,   in3";
        tags.exclude = "ex1,  ex2  ,  ex3";
        System.out.println(tags.getFormattedTags());
        tags.include = "in1  ";
        tags.exclude = null;
        System.out.println(tags.getFormattedTags());
    }
}
