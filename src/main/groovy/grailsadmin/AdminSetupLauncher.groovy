package grailsadmin

/** Starts the Grails command without relying on Gradle's JavaExec argument handling. */
class AdminSetupLauncher {
    static void main(String[] ignored) {
        String applicationClass = System.getProperty('grails.admin.setup.applicationClass')
        if (!applicationClass) {
            System.err.println('Missing Grails application class for admin setup')
            System.exit(1)
        }

        Class runner = Class.forName('grails.ui.command.GrailsApplicationContextCommandRunner')
        Object[] invocationArguments = new Object[1]
        invocationArguments[0] = ['admin-setup', applicationClass] as String[]
        runner.getMethod('main', String[].class).invoke(null, invocationArguments)
    }
}
