package com.mborzenkov.jqfgradleplugin.task

import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class JqfFuzzTask extends DefaultTask {

    @TaskAction
    def fuzz() {
        println "JQF is configured properly with guidance: ${GuidedFuzzing.currentGuidance}"
    }
}
