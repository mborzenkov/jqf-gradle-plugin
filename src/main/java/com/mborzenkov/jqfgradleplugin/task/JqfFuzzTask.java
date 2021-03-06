/* START OF JQF Authors' Copyright
 *
 * Copyright (c) 2017-2018 The Regents of the University of California
 * Copyright (c) 2020-2021 Rohan Padhye
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * END OF JQF Authors' Copyright
 *
 * Authors of this file used source code from https://github.com/rohanpadhye/JQF following the
 *  copyright notice provided above.
 */
package com.mborzenkov.jqfgradleplugin.task;

import static edu.berkeley.cs.jqf.instrument.InstrumentingClassLoader.stringsToUrls;

import com.mborzenkov.jqfgradleplugin.JqfEngine;
import com.mborzenkov.jqfgradleplugin.JqfUtil;
import edu.berkeley.cs.jqf.fuzz.ei.ExecutionIndexingGuidance;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.instrument.InstrumentingClassLoader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.junit.runner.Result;

/**
 * Fuzz task for JQF.
 */
public class JqfFuzzTask extends DefaultTask {

  public static final String USAGE =
      "./gradlew fuzz --class=com.example.YourTestClass --method=yourTestMethod";
  public static final String HELP = "see ./gradlew help --task fuzz";
  
  private String target;
  private String testClassName;
  private String testMethod;
  private String excludes;
  private String includes;
  private String time;
  private Duration duration;
  private boolean blind;
  private JqfEngine engine;
  private boolean disableCoverage;
  private String inputDirectory;
  private String outputDirectory;
  private boolean saveAll;
  private boolean libFuzzerCompatOutput;
  private boolean quiet;
  private boolean exitOnCrash;
  private String runTimeout;
  private int runTimeoutMs;
  private boolean fixedSizeInputs;

  @Option(option = "target", description = "")
  void setTarget(String target) {
    this.target = target;
  }

  @Option(
      option = "class",
      description = "The fully-qualified name of the test class containing methods to fuzz.\n\n"
          + "This class will be loaded using the project's test classpath. It must be "
          + "annotated with @RunWith(JQF.class)."
  )
  void setTestClassName(String testClassName) {
    this.testClassName = testClassName;
  }

  @Option(
      option = "method",
      description = "The name of the method to fuzz.\n"
          + "This method must be annotated with @Fuzz, and take one or more arguments "
          + "(with optional junit-quickcheck annotations) whose values will be fuzzed by JQF.\n\n"
          + " If more than one method of this name exists in the test class or if the method is "
          + "not declared `public void`, then the fuzzer will not launch."
  )
  void setTestMethod(String testMethod) {
    this.testMethod = testMethod;
  }

  @Option(
      option = "excludes",
      description = "Comma-separated list of FQN prefixes to exclude from coverage.\n\n"
          + "Example: `org/mozilla/javascript/gen,org/slf4j/logger`, will exclude classes "
          + "auto-generated by Mozilla Rhino's CodeGen and logging classes."
  )
  void setExcludes(String excludes) {
    this.excludes = excludes;
  }

  @Option(
      option = "includes",
      description = "Comma-separated list of FQN prefixes to forcibly include, even if they"
          + "match an exclude.\n\n"
          + "Typically, these will be a longer prefix than a prefix in the excludes clauses."
  )
  void setIncludes(String includes) {
    this.includes = includes;
  }

  @Option(
      option = "time",
      description = "The duration of time for which to run fuzzing.\n\n"
          + "If this property is not provided, the fuzzing session is run for an unlimited time "
          + "until the process is terminated by the user (e.g. via kill or CTRL+C).\n\n"
          + "Valid time durations are non-empty strings in the format [Nh][Nm][Ns], "
          + "such as \"60s\" or \"2h30m\"."
  )
  void setTime(String time) {
    this.time = time;
  }

  @Option(
      option = "blind",
      description = "Whether to generate inputs blindly without taking into account coverage "
          + "feedback. Blind input generation is equivalent to running QuickCheck.\n\n"
          + "If this property is set to `true`, then the fuzzing algorithm does not "
          + "maintain a queue. Every input is randomly generated from scratch. The program under "
          + "test is still instrumented in order to provide coverage statistics. This mode is "
          + "mainly useful for comparing coverage-guided fuzzing with plain-old QuickCheck."
  )
  void setBlind(boolean blind) {
    this.blind = blind;
  }

  @Option(
      option = "engine",
      description = "The fuzzing engine.\n\nOne of 'zest' and 'zeal'. Default is 'zest'."
  )
  public void setEngine(JqfEngine engine) {
    this.engine = engine;
  }

  @Option(
      option = "noCov",
      description = "Whether to disable code-coverage instrumentation.\n\n"
          + "Disabling instrumentation speeds up test case execution, but provides no feedback "
          + "about code coverage in the status screen and to the fuzzing guidance.\n\n"
          + "This setting only makes sense when used with --blind."
  )
  public void setDisableCoverage(boolean disableCoverage) {
    this.disableCoverage = disableCoverage;
  }

  @Option(
      option = "in",
      description = "The name of the input directory containing seed files.\n\n"
          + "If not provided, then fuzzing starts with randomly generated initial inputs."
  )
  public void setInputDirectory(String inputDirectory) {
    this.inputDirectory = inputDirectory;
  }

  @Option(
      option = "out",
      description = "The name of the output directory where fuzzing results will be stored.\n\n"
          + "The directory will be created inside the standard project build directory.\n\n"
          + "If not provided, defaults to `jqf-fuzz/${testClassName}/${$testMethod}`."
  )
  public void setOutputDirectory(String outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  @Option(
      option = "saveAll",
      description = "Whether to save ALL inputs generated during fuzzing, even the ones that "
          + "do not have any unique code coverage.\n\n"
          + "This setting leads to a very large number of files being created in the output "
          + "directory, and could potentially reduce the overall performance of fuzzing."
  )
  public void setSaveAll(boolean saveAll) {
    this.saveAll = saveAll;
  }

  @Option(
      option = "libFuzzerCompatOutput",
      description = "Weather to use libFuzzer like output instead of AFL like stats screen\n\n"
          + "If this property is set to `true`, then output will look like libFuzzer output "
          + "https://llvm.org/docs/LibFuzzer.html#output"
  )
  public void setLibFuzzerCompatOutput(boolean libFuzzerCompatOutput) {
    this.libFuzzerCompatOutput = libFuzzerCompatOutput;
  }

  @Option(
      option = "quiet",
      description = "Whether to avoid printing fuzzing statistics progress in the console.\n\n"
          + "If not provided, defaults to `false`."
  )
  public void setQuiet(boolean quiet) {
    this.quiet = quiet;
  }

  @Option(
      option = "exitOnCrash",
      description = "Whether to stop fuzzing once a crash is found.\n\n"
          + "If this property is set to <code>true</code>, then the fuzzing will exit on first "
          + "crash. Useful for continuous fuzzing when you dont wont to consume resource "
          + "once a crash is found. Also fuzzing will be more effective once the crash is fixed."
  )
  public void setExitOnCrash(boolean exitOnCrash) {
    this.exitOnCrash = exitOnCrash;
  }

  @Option(
      option = "runTimeout",
      description = "The timeout for each individual trial, in milliseconds.\n\n"
          + "If not provided, defaults to 0 (unlimited)."
  )
  public void setRunTimeout(String runTimeout) {
    this.runTimeout = runTimeout;
  }

  @Option(
      option = "fixedSize",
      description = "Whether to bound size of inputs being mutated by the fuzzer.\n\n"
          + "If this property is set to true, then the fuzzing engine will treat inputs as "
          + "fixed-size arrays of bytes rather than as an infinite stream of pseudo-random "
          + "choices. This option is appropriate when fuzzing test methods that take a single "
          + "argument of type `java.io.InputStream` and that also provide a set of seed inputs "
          + "via the `in' property.\n\n"
          + "If not provided, defaults to `false`."
  )
  public void setFixedSizeInputs(boolean fixedSizeInputs) {
    this.fixedSizeInputs = fixedSizeInputs;
  }

  @TaskAction
  void fuzz() {
    validateOptions();

    final Project project = getProject();
    final Logger logger = project.getLogger();

    ClassLoader loader;
    ZestGuidance guidance;
    Result result;

    final File target = this.target == null || this.target.isEmpty()
        ? project.getBuildDir() : new File(this.target);
    final PrintStream out = logger.isDebugEnabled() ? System.out : null;

    // Configure classes to instrument
    if (excludes != null) {
      System.setProperty("janala.excludes", excludes);
    }
    if (includes != null) {
      System.setProperty("janala.includes", includes);
    }

    // Configure Zest Guidance
    if (saveAll) {
      System.setProperty("jqf.ei.SAVE_ALL_INPUTS", "true");
    }
    if (libFuzzerCompatOutput) {
      System.setProperty("jqf.ei.LIBFUZZER_COMPAT_OUTPUT", "true");
    }
    if (quiet) {
      System.setProperty("jqf.ei.QUIET_MODE", "true");
    }
    if (exitOnCrash) {
      System.setProperty("jqf.ei.EXIT_ON_CRASH", "true");
    }
    if (runTimeoutMs > 0) {
      System.setProperty("jqf.ei.TIMEOUT", String.valueOf(runTimeout));
    }
    if (fixedSizeInputs) {
      System.setProperty("jqf.ei.GENERATE_EOF_WHEN_OUT", "true");
    }

    if (outputDirectory == null || outputDirectory.isEmpty()) {
      outputDirectory =
          "fuzz-results" + File.separator + testClassName + File.separator + testMethod;
    }

    try {
      List<String> classpathElements = JqfUtil.getTestClasspathElements(project);

      if (disableCoverage) {
        loader = new URLClassLoader(
            stringsToUrls(classpathElements.toArray(new String[0])),
            getClass().getClassLoader());

      } else {
        loader = new InstrumentingClassLoader(
            classpathElements.toArray(new String[0]),
            getClass().getClassLoader());
      }
    } catch (GradleException | MalformedURLException e) {
      throw new GradleException("Could not get project classpath", e);
    }

    File resultsDir = new File(target, outputDirectory);
    String targetName = testClassName + "#" + testMethod;
    File seedsDir = inputDirectory == null ? null : new File(inputDirectory);
    engine = engine == null ? JqfEngine.ZEST : engine;
    try {
      switch (engine) {
        case ZEST:
          guidance = new ZestGuidance(targetName, duration, resultsDir, seedsDir);
          break;
        case ZEAL:
          System.setProperty("jqf.traceGenerators", "true");
          guidance = new ExecutionIndexingGuidance(targetName, duration, resultsDir, seedsDir);
          break;
        default:
          throw new GradleException(
              "Option --engine is invalid, " + HELP + "\n" + USAGE + " --engine=zest");
      }
      guidance.setBlind(blind);
    } catch (FileNotFoundException e) {
      throw new GradleException("File not found", e);
    } catch (IOException e) {
      throw new GradleException("I/O error", e);
    }

    try {
      result = GuidedFuzzing.run(testClassName, testMethod, loader, guidance, out);
    } catch (ClassNotFoundException e) {
      throw new GradleException("Could not load test class", e);
    } catch (IllegalArgumentException e) {
      throw new GradleException("Bad request", e);
    } catch (RuntimeException e) {
      throw new GradleException("Internal error", e);
    }

    if (!result.wasSuccessful()) {
      Throwable e = result.getFailures().get(0).getException();
      if (result.getFailureCount() == 1) {
        if (e instanceof GuidanceException) {
          throw new GradleException("Internal error", e);
        }
      }
      throw new GradleException(String.format("Fuzzing resulted in the test failing on "
          + "%d input(s). Possible bugs found.\n\nUse mvn jqf:repro to reproduce failing test cases "
          + "from %s/failures. ", result.getFailureCount(), resultsDir)
          + "\n\nSample exception included with this message.", e);
    }
  }

  private void validateOptions() {
    if (testClassName == null || testClassName.isEmpty()) {
      throw new GradleException("Option --class is required.\n" + USAGE);
    }
    if (testMethod == null || testMethod.isEmpty()) {
      throw new GradleException("Option --method is required.\n" + USAGE);
    }
    if (runTimeout != null && !runTimeout.isEmpty()) {
      try {
        runTimeoutMs = Integer.parseInt(runTimeout);
      } catch (NumberFormatException e) {
        throw new GradleException(
            "Option --runTimeout must be int.\n" + USAGE + " --runTimeout=10");
      }
    } else {
      runTimeoutMs = 0;
    }
    if (time != null && !time.isEmpty()) {
      try {
        duration = Duration.parse("PT" + time);
      } catch (DateTimeParseException e) {
        throw new GradleException(
            "Option --time is invalid, " + HELP + "\n" + USAGE + " --time=10s");
      }
    }
  }
}
