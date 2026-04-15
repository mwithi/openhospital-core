/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2026 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
 *
 * Open Hospital is a free and open source software for healthcare data management.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * https://www.gnu.org/licenses/gpl-3.0-standalone.html
 */
package org.isf.plugin.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.isf.plugin.model.PluginDescriptor;
import org.isf.plugin.spi.OHPlugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Generates {@code manifest.json} from the plugin's {@link OHPlugin} implementation.
 *
 * <p>Binds to the {@code prepare-package} phase so the manifest is written
 * into {@code target/classes/} before the JAR is assembled.
 */
@Mojo(
    name                  = "generate-manifest",
    defaultPhase          = LifecyclePhase.PREPARE_PACKAGE,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe            = true
)
public class GenerateManifestMojo extends AbstractMojo {

    /**
     * The Maven project being built. Injected automatically by Maven.
     * Used to read the compile classpath.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Directory where compiled classes are placed.
     * Defaults to {@code target/classes}.
     */
    @Parameter(
        defaultValue = "${project.build.outputDirectory}",
        readonly     = true,
        required     = true
    )
    private File outputDirectory;

    /**
     * Name of the generated manifest file. Changing this is not recommended —
     * the PluginRegistryImpl in openhospital-api expects exactly "manifest.json".
     */
    @Parameter(defaultValue = "manifest.json")
    private String manifestFileName;

    /**
     * Set to {@code true} to skip manifest generation entirely.
     * Useful for CI pipelines that build the SPI itself.
     */
    @Parameter(defaultValue = "false", property = "oh.plugin.skipManifest")
    private boolean skip;

    // -------------------------------------------------------------------------

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("oh:generate-manifest skipped (oh.plugin.skipManifest=true)");
            return;
        }

        getLog().info("oh:generate-manifest — generating " + manifestFileName);

        URLClassLoader classLoader = buildClassLoader();
        List<OHPlugin> plugins     = loadPlugins(classLoader);

        if (plugins.isEmpty()) {
            throw new MojoFailureException(
                "oh:generate-manifest — no OHPlugin implementation found. " +
                "Make sure the class is registered in " +
                "META-INF/services/org.isf.plugin.spi.OHPlugin");
        }
        if (plugins.size() > 1) {
            throw new MojoFailureException(
                "oh:generate-manifest — found " + plugins.size() +
                " OHPlugin implementations. Each plugin JAR must contain exactly one.");
        }

        OHPlugin plugin = plugins.get(0);
        PluginDescriptor descriptor;
        try {
            descriptor = plugin.getDescriptor();
        } catch (Exception e) {
            throw new MojoExecutionException(
                "oh:generate-manifest — getDescriptor() threw an exception on " +
                plugin.getClass().getName(), e);
        }

        if (descriptor == null) {
            throw new MojoFailureException(
                "oh:generate-manifest — getDescriptor() returned null on " +
                plugin.getClass().getName());
        }

        writeManifest(descriptor);

        getLog().info("oh:generate-manifest — written " + manifestFileName +
                      " for plugin '" + descriptor.getPluginId() +
                      "' v" + descriptor.getVersion());
    }

    // -------------------------------------------------------------------------

    private URLClassLoader buildClassLoader() throws MojoExecutionException {
        List<URL> urls = new ArrayList<>();
        try {
            urls.add(outputDirectory.toURI().toURL());
            for (String element : project.getCompileClasspathElements()) {
                urls.add(new File(element).toURI().toURL());
            }
        } catch (Exception e) {
            throw new MojoExecutionException(
                "oh:generate-manifest — failed to build classpath", e);
        }
        return new URLClassLoader(
            urls.toArray(new URL[0]),
            Thread.currentThread().getContextClassLoader());
    }

    private List<OHPlugin> loadPlugins(URLClassLoader classLoader)
            throws MojoExecutionException {
        ServiceLoader<OHPlugin> loader =
                ServiceLoader.load(OHPlugin.class, classLoader);
        List<OHPlugin> found = new ArrayList<>();
        try {
            for (OHPlugin plugin : loader) {
                found.add(plugin);
                getLog().debug("oh:generate-manifest — found " +
                               plugin.getClass().getName());
            }
        } catch (Exception e) {
            throw new MojoExecutionException(
                "oh:generate-manifest — failed to load OHPlugin implementations", e);
        }
        return found;
    }

    private void writeManifest(PluginDescriptor descriptor)
            throws MojoExecutionException {
        Path target = outputDirectory.toPath().resolve(manifestFileName);
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, DescriptorSerializer.toJson(descriptor));
        } catch (Exception e) {
            throw new MojoExecutionException(
                "oh:generate-manifest — failed to write " + target, e);
        }
    }
}
