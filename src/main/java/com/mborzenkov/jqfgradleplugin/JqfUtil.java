package com.mborzenkov.jqfgradleplugin;

import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Project;

/**
 * Utility functions for JQF.
 */
public class JqfUtil {

  /**
   * Returns a list of classpath elements with test and main sources.
   * @return list that contains
   *    main output dir
   *    test output dir
   *    classes dir
   */
  public static List<String> getTestClasspathElements(Project project) {
    String buildDir = project.getBuildDir().getAbsolutePath();
    List<String> list = new ArrayList<>(2);
    list.add(buildDir + "/classes");
    list.add(buildDir + "/classes/java/test");
    list.add(buildDir + "/classes/java/main");
    return list;
  }
}
