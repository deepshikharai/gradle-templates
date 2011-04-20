package templates

import org.gradle.api.Plugin
import org.gradle.api.Project

class TemplatesPlugin implements Plugin<Project> {

   static final String group = "Template"
   static final String lineSep = System.getProperty("line.separator")
   static final String inputPrompt = "${lineSep}??>"

   static void prependPlugin(String plugin, File gradleFile) {
      def oldText = gradleFile.text
      gradleFile.text = ""
      gradleFile.withPrintWriter { pw ->
         pw.println "apply plugin: \"${plugin}\""
         pw.print oldText
      }
   }

   static String prompt(String message, String defaultValue = null) {
      if (defaultValue) {
         return System.console().readLine("${inputPrompt} ${message} [${defaultValue}] ") ?: defaultValue
      }
      return System.console().readLine("${inputPrompt} ${message} ") ?: defaultValue
   }

   static int promptOptions(String message, List options = []) {
      promptOptions(message, "", options)
   }

   static int promptOptions(String message, int defaultValue, List options = []) {
      String consoleMessage = "${inputPrompt} ${message}"
      consoleMessage += "${lineSep}    Pick an option ${1..options.size()}"
      options.eachWithIndex { option, index ->
         consoleMessage += "${lineSep}     (${index + 1}): ${option}"
      }
      if (defaultValue) {
         consoleMessage += "${inputPrompt} [${defaultValue}] "
      } else {
         consoleMessage += "${inputPrompt} "
      }
      try {
         def range = 0..options.size() - 1
         int choice = Integer.parseInt(System.console().readLine(consoleMessage) ?: "${defaultValue}") - 1
         if(range.containsWithinBounds(choice)) {
            return choice
         } else {
            throw new IllegalArgumentException("Option is not valid.")
         }
      } catch (Exception e) {
         throw new IllegalArgumentException("Option is not valid.", e)
      }
   }

   static boolean promptYesOrNo(String message, boolean defaultValue = false) {
      def defaultStr = defaultValue ? "Y" : "n"
      String consoleVal = System.console().readLine("${inputPrompt} ${message} (Y|n)[${defaultStr}] ")
      if (consoleVal) {
         return consoleVal.toLowerCase().startsWith("y")
      }
      return defaultValue
   }

   def void apply(Project project) {
      project.convention.plugins.templatePlugin = new TemplatesPluginConvention()
      project.apply(plugin: 'java-templates')
      project.apply(plugin: 'groovy-templates')
      project.apply(plugin: 'webapp-templates')

      project.task("gradlePluginInputs") << {
         String lProjectName = project.name.toLowerCase()
         String cProjectName = project.name.capitalize();
         TemplatesPluginConvention convention = project.convention.plugins.templatePlugin
         if (!convention.gradlePluginApplyLabel) {
            convention.gradlePluginApplyLabel = prompt("Plugin 'apply' label?(${lProjectName})") ?: lProjectName
         }
         if (!convention.gradlePluginClassName) {
            convention.gradlePluginClassName = prompt("Plugin class name?(${lProjectName}.${cProjectName})") ?: "${lProjectName}.${cProjectName}"
         }
      }
      project.task("initGradlePlugin", dependsOn: ["gradlePluginInputs", "initGroovyProject"]) << {
         TemplatesPluginConvention convention = project.convention.plugins.templatePlugin
         String pluginApplyLabel = convention.gradlePluginApplyLabel
         String pluginClassName = convention.gradlePluginClassName

         ProjectTemplate.fromUserDir() {
            "src/main/" {
               "resources/META-INF/gradle-plugins" {
                  "${pluginApplyLabel}.properties" "implementation-class=${pluginClassName}"
               }
               "groovy" {
                  if (pluginClassName.indexOf(".")) {
                     def parts = pluginClassName.split("\\.") as List
                     def className = parts.pop()
                     def path = parts.join(File.separator)
                     "${path}" {
                        "${className}.groovy" """
                        package ${parts.join('.')}
                        class ${className} implements Plugin<Project> {
                           void apply (Project project) {
                              // add your plugin tasks here.
                           }
                        }"""
                     }
                  }
               }
            }
         }
      }
      /*
      project.task("init-war-project") << {
         ProjectTemplate.fromUserDir() {
            "src" {
               "main" {
                  "java" {}
                  "resources" {}
                  "webapp" {
                     "WEB-INF" {
                        "web.xml" """
                        <?xml version="1.0" encoding="ISO-8859-1"?>
                        <!DOCTYPE web-app
                            PUBLIC "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
                            "http://java.sun.com/dtd/web-app_2_3.dtd">

                        <web-app>
                           <!-- General description of your web application -->
                           <display-name>${project.name} - Webapp</display-name>
                              <description>
                                 Describe the ${project.name} Webapp here.
                                 ${System.getProperty('user.name')}@example.com

                                 See http://tomcat.apache.org/tomcat-6.0-doc/appdev/web.xml.txt for more information
                                 regarding this Web Descriptor File.
                           </description>
                        </web-app>
                        """
                     }
                  }
               }
               "test" {
                  "java" {}
                  "resources" {}
               }
            }
            "LICENSE.txt" "// Your License Goes here"
         }

         prependPlugin "war", new File("build.gradle")
      }
      */
   }
}