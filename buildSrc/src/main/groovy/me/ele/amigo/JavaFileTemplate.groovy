package me.ele.amigo

class JavaFileTemplate {

    String appName


    def getContent() {
        return """
package me.ele.amigo;

public class acd {
    public static final String n = ${appName}.class.getName();
}
"""
    }
}