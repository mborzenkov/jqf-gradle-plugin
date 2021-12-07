package com.mborzenkov.jqfgradleplugin

import com.mborzenkov.jqfgradleplugin.task.JqfFuzzTask
import com.mborzenkov.jqfgradleplugin.task.JqfReproTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Main JQF Gradle Plugin class.
 */
class JqfGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.tasks.create('fuzz', JqfFuzzTask) {
            group = 'JQF'
            description = 'Performs code-coverage-guided generator-based fuzz testing ' +
                    'using a provided entry point.\n' +
                    'Required options: --class, --method ' +
                    'USAGE: ' + USAGE
        }
        project.tasks.create('repro', JqfReproTask) {
            group = 'JQF'
            description = 'Replays a test case produced by JQF.\n' +
                    'Required options: --class, --method, --input ' +
                    'USAGE: ' + USAGE
        }
    }
}
