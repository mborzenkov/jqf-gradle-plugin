package com.mborzenkov.jqfgradleplugin

import com.mborzenkov.jqfgradleplugin.task.JqfFuzzTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class JqfGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create('jqf', JqfGradleExtension)
        project.tasks.create('fuzz', JqfFuzzTask) {
            group = 'JQF'
            description = 'Performs code-coverage-guided generator-based fuzz testing ' +
                    'using a provided entry point.'
        }
    }
}
