package jabsc.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import jabsc.Compiler;

/**
 * Generates Java source from ABS.
 */
@Mojo(name = "jabsc", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CompileMojo extends AbstractMojo {

  private static final String ABS_EXTENSION_GLOB = "*.abs";
  private static final String DEFAULT_ABS_SOURCE_DIRECTORY = "${basedir}/src/main/abs";
  private static final String STANDARD_GENERATED_SOURCES_DIRECTORY_NAME = "generated-sources";
  private static final String DEFAULT_JABSC_GENERATED_DIRECTORY_NAME = "jabsc";
  private static final String DEFAULT_JABSC_GENERATED_DIRECTORY = "${project.build.directory}/"
      + STANDARD_GENERATED_SOURCES_DIRECTORY_NAME + "/" + DEFAULT_JABSC_GENERATED_DIRECTORY_NAME;

  @Parameter(defaultValue = "${project}")
  private MavenProject project;

  @Parameter(defaultValue = DEFAULT_JABSC_GENERATED_DIRECTORY)
  private File outputDirectory;

  @Parameter(defaultValue = DEFAULT_ABS_SOURCE_DIRECTORY)
  private File sourceDirectory;

  public void execute() throws MojoExecutionException {
    final String projectBuildDirectory = this.project.getBuild().getDirectory();
    final Path output = resolveOutputDirectory(this.outputDirectory, projectBuildDirectory);
    project.addCompileSourceRoot(output.toAbsolutePath().toString());

    final List<Path> sources = collectSources(sourceDirectory.toPath());
    try {
      Compiler compiler = new Compiler(sources, output);
      getLog().info("Compiling " + sources.size() + " ABS sources to " + output);
      List<Path> paths = compiler.compile();
      getLog().info("Compiled " + paths.size() + " ABS sources to " + output);
    } catch (Exception e) {
      throw new MojoExecutionException("Compilation failed", e);
    }
  }

  /**
   * Setter
   * 
   * @param project the {@link MavenProject} for this mojo
   */
  public void setProject(MavenProject project) {
    this.project = project;
  }

  /**
   * Setter
   * 
   * @param outputDirectory the relative path to
   *        <code>${basedir}</code> to write Java sources to
   */
  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  /**
   * Setter
   * 
   * @param sourceDirectory the relative path to
   *        <code>${basedir}</code> to read ABS sources from
   */
  public void setSourceDirectory(File sourceDirectory) {
    this.sourceDirectory = sourceDirectory;
  }

  /**
   * Collects ABS sources to compile to Java.
   * 
   * @param directory the path to the directory containing ABS
   *        sources
   * @return the list of all ABS sources to be compiled to Java
   * @throws MojoExecutionException
   */
  protected List<Path> collectSources(Path directory) throws MojoExecutionException {
    List<Path> sources = new ArrayList<>();
    if (!Files.exists(directory)) {
      return sources;
    }
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(directory, ABS_EXTENSION_GLOB)) {
      ds.forEach(p -> sources.add(p));
      return sources;
    } catch (IOException e) {
      String message = "Cannot read *.abs sources from " + directory;
      throw new MojoExecutionException(message, e);
    }
  }

  /**
   * Resolves the effective directory to write Java sources to.
   * 
   * @param configuredOutputDirectory the user configured path
   *        to generated source directory
   * @param projectBuildDirectory the {@link MavenProject}'s
   *        build directory full path
   * @return the directory to which Java sources should be
   *         written to
   */
  protected Path resolveOutputDirectory(File configuredOutputDirectory,
      String projectBuildDirectory) {
    if (configuredOutputDirectory != null) {
      return configuredOutputDirectory.toPath().toAbsolutePath();
    }
    return Paths.get(projectBuildDirectory).resolve(STANDARD_GENERATED_SOURCES_DIRECTORY_NAME)
        .resolve(DEFAULT_JABSC_GENERATED_DIRECTORY_NAME);
  }

}
