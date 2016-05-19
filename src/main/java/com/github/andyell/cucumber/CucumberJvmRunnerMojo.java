package com.github.andyell.cucumber;

import com.google.common.base.CaseFormat;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@SuppressWarnings({"WeakerAccess", "unused"})
@Mojo(name = "generate-runners", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
public class CucumberJvmRunnerMojo extends AbstractMojo {

    @Parameter(required = true, readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(required = false, alias = "generated-source-path",
            defaultValue = "${project.build.directory}" + DEFAULT_SRC_DIR_PATH
    )
    private File generatedSourcePath;

    @Parameter(alias = "cucumber-options", required = false)
    private CucumberOptions cucumberOptions;

    @Parameter(alias = "test-pattern", required = false, defaultValue = DEFAULT_TEST_PATTERN)
    private String testPattern;

    @Parameter(alias = "append-test-pattern", required = false, defaultValue = "true")
    private boolean appendTestPattern;

    private Template template;
    private VelocityContext context;
    private Path testResourceDir;

    private static final String DEFAULT_TEST_PATTERN = "IT";
    private static final String DEFAULT_FEATURES_RELATIVE_PATH = "features";
    private static final String DEFAULT_SRC_DIR_PATH = "/generated-test-sources/cucumber-runners";

    public void execute() throws MojoExecutionException, MojoFailureException {
        Resource testResource = (Resource) project.getTestResources().get(0);
        testResourceDir = Paths.get(testResource.getDirectory());

        getLog().info("Test resource directory: " + testResourceDir.toString());

        if (!testResourceDir.toFile().exists()) {
            throw new MojoExecutionException("Unable to determine test resource directory.");
        }

        if (cucumberOptions == null) {
            cucumberOptions = CucumberOptions.DEFAULT_OPTIONS;
        }

        Path featurePath;
        if (cucumberOptions.getFeaturesPath() != null && cucumberOptions.getFeaturesPath().exists()) {
            featurePath = cucumberOptions.getFeaturesPath().toPath();
            getLog().info("Feature files directory: " + featurePath.toString());
        } else {
            // Generate a default and see if that exists instead
            featurePath = testResourceDir.resolve(DEFAULT_FEATURES_RELATIVE_PATH);
            getLog().info("Feature files directory: " + featurePath.toString());
            if (!Files.exists(featurePath)) {
                throw new MojoExecutionException("Unable to determine features directory.");
            }
        }

        if (generatedSourcePath == null) {
            generatedSourcePath = new File(project.getBuild().getDirectory() + DEFAULT_SRC_DIR_PATH);
        }

        project.addTestCompileSourceRoot(generatedSourcePath.getAbsolutePath());

        getLog().info("Generated source files directory: " + generatedSourcePath.getAbsolutePath());

        initialiseTemplate();
        initialiseVelocityContext();

        try {
            DirectoryStream<Path> featureFiles = Files.newDirectoryStream(featurePath, "*.feature");
            for (Path featureFilePath : featureFiles) {
                logDebug("Generating source file for feature file: " + featureFilePath.toString());
                generateSourceFile(featureFilePath);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Got an exception trying to process the feature files.", e);
        }
    }

    private void generateSourceFile(Path featureFilePath) throws IOException {
        String featureName = getFeatureName(featureFilePath);
        logDebug("Converted feature file name: " + featureName);
        Path sourceFile = getSourceFile(featureName);
        logDebug("Writing source file as: " + sourceFile.toString());
        FileWriter writer = new FileWriter(sourceFile.toFile());

        context.put("class", featureName);
        context.put("feature", getFeature(featureFilePath));

        template.merge(context, writer);
        writer.close();

        logDebug("Completed writing source file: " + sourceFile.toString());
    }

    private String getFeatureName(Path featureFilePath) {
        String rootName = featureFilePath.getFileName().toString().replace(".feature", "").toLowerCase();
        String camelCaseName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, rootName);
        return appendTestPattern ? camelCaseName + testPattern : testPattern + camelCaseName;
    }

    private String getFeature(Path featureFilePath) {
        return "\"classpath:" + getTestResourceRelativePath(featureFilePath).replace("\\", "/") + "\"";
    }

    private String getTestResourceRelativePath(Path featureFilePath) {
        return testResourceDir.relativize(featureFilePath).toString();
    }

    private Path getSourceFile(String featureName) throws IOException {
        if (!generatedSourcePath.exists()) {
            Files.createDirectories(generatedSourcePath.toPath());
        }
        return Files.createFile(generatedSourcePath.toPath().resolve(featureName + ".java"));
    }

    private void initialiseTemplate() {
        final Properties props = new Properties();
        props.put("resource.loader", "class");
        props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        final VelocityEngine engine = new VelocityEngine(props);
        engine.init();
        template = engine.getTemplate("junit-cucumber-runner.vm");
    }

    private void initialiseVelocityContext() {
        context = new VelocityContext();
        context.put("dryRun", cucumberOptions.isDryRun());
        context.put("strict", cucumberOptions.isStrict());
        context.put("monochrome", cucumberOptions.isMonochrome());
        context.put("glue", cucumberOptions.getFormattedGlue());
        context.put("plugin", cucumberOptions.getFormattedPlugin());
        context.put("name", cucumberOptions.getFormattedName());
        context.put("tags", cucumberOptions.getFormattedTags());
    }

    private void logDebug(String text) {
        if (getLog().isDebugEnabled()) {
            getLog().debug(text);
        }
    }

}
