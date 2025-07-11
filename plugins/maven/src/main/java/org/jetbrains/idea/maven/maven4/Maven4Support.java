// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.idea.maven.maven4;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.maven.server.telemetry.MavenServerTelemetryClasspathUtil;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.NioFiles;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.MavenVersionAwareSupportExtension;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.BundledMaven4;
import org.jetbrains.idea.maven.project.StaticResolvedMavenHomeType;
import org.jetbrains.idea.maven.server.MavenDistribution;
import org.jetbrains.idea.maven.server.MavenDistributionsCache;
import org.jetbrains.idea.maven.server.MavenServerManager;
import org.jetbrains.idea.maven.utils.MavenLog;
import org.jetbrains.idea.maven.utils.MavenUtil;
import org.jetbrains.intellij.build.dependencies.BuildDependenciesCommunityRoot;
import org.jetbrains.intellij.build.impl.BundledMavenDownloader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.jetbrains.idea.maven.utils.MavenUtil.locateModuleOutput;


final class Maven4Support implements MavenVersionAwareSupportExtension {
  private static final @NonNls String MAIN_CLASS40 = "com.intellij.maven.server.m40.RemoteMavenServer40";

  @Override
  public boolean isSupportedByExtension(@Nullable File mavenHome) {
    String version = mavenHome == null ? null : MavenUtil.getMavenVersion(mavenHome.toPath());
    return StringUtil.compareVersionNumbers(version, "4") >= 0;
  }

  @Override
  public @Nullable Path getMavenHomeFile(@Nullable StaticResolvedMavenHomeType mavenHomeType) {
    if (mavenHomeType == null) return null;
    if (mavenHomeType == BundledMaven4.INSTANCE) {
      return MavenDistributionsCache.resolveEmbeddedMavenHome().getMavenHome();
    }
    return null;
  }

  @Override
  public @NotNull List<Path> collectClassPathAndLibsFolder(@NotNull MavenDistribution distribution) {
    final Path pluginFileOrDir = Path.of(PathUtil.getJarPathForClass(MavenServerManager.class));
    final List<Path> classpath = new ArrayList<>();

    if (MavenUtil.isRunningFromSources()) {
      MavenLog.LOG.debug("collecting classpath for local run");
      prepareClassPathForLocalRunAndUnitTests(distribution.getVersion(), classpath);
    }
    else {
      MavenLog.LOG.debug("collecting classpath for production");
      prepareClassPathForProduction(distribution.getVersion(), classpath, pluginFileOrDir.getParent());
    }

    addMavenLibs(classpath, distribution.getMavenHome());
    MavenLog.LOG.debug("Collected classpath = ", classpath);
    return classpath;
  }

  private static void prepareClassPathForProduction(@NotNull String mavenVersion,
                                                    List<Path> classpath,
                                                    Path rootPath) {

    classpath.add(PathManager.getJarForClass(MavenId.class));
    classpath.add(rootPath.resolve("maven-server.jar"));

    classpath.add(rootPath.resolve("maven-server-telemetry.jar"));
    try {
      classpath.add(PathManager.getJarForClass(Class.forName("io.opentelemetry.sdk.trace.export.SpanExporter")));
    }
    catch (ClassNotFoundException e) {
      MavenLog.LOG.error(e);
    }
    addDir(classpath, rootPath.resolve("maven-telemetry-lib"), f -> true);

    addDir(classpath, rootPath.resolve("maven4-server-lib"), f -> true);

    classpath.add(rootPath.resolve("maven40-server.jar"));
  }

  private static void prepareClassPathForLocalRunAndUnitTests(@NotNull String mavenVersion, List<Path> classpath) {
    BuildDependenciesCommunityRoot communityRoot = new BuildDependenciesCommunityRoot(Path.of(PathManager.getCommunityHomePath()));
    BundledMavenDownloader.INSTANCE.downloadMaven4LibsSync(communityRoot);
    
    classpath.add(PathManager.getJarForClass(MavenId.class));
    classpath.add(locateModuleOutput("intellij.maven.server"));

    classpath.add(locateModuleOutput("intellij.maven.server.telemetry"));
    classpath.addAll(MavenUtil.collectClasspath(MavenServerTelemetryClasspathUtil.TELEMETRY_CLASSES));

    Path parentPath = MavenUtil.getMavenPluginParentFile();
    addDir(classpath, parentPath.resolve("maven40-server-impl/lib"), f -> true);

    classpath.add(locateModuleOutput("intellij.maven.server.m40"));
  }

  private static void addMavenLibs(List<Path> classpath, Path mavenHome) {
    addDir(classpath, mavenHome.resolve("lib"), f -> !f.getFileName().toString().contains("maven-slf4j-provider"));
    Path bootFolder = mavenHome.resolve("boot");
    List<Path> classworldsJars =
      NioFiles.list(bootFolder).stream().filter((f) -> StringUtil.contains(f.getFileName().toString(), "classworlds")).toList();
    classpath.addAll(classworldsJars);
  }

  private static void addDir(List<Path> classpath, Path dir, Predicate<Path> filter) {
    List<Path> files = NioFiles.list(dir);

    for (Path jar : files) {
      if (Files.isRegularFile(jar) && jar.getFileName().toString().endsWith(".jar") && filter.test(jar)) {
        classpath.add(jar);
      }
    }
  }

  @Override
  public String getMainClass(MavenDistribution distribution) {
    return MAIN_CLASS40;
  }
}
