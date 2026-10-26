package grailsadmin

import grails.dev.commands.ApplicationCommand
import grails.dev.commands.ExecutionContext
import java.nio.charset.StandardCharsets

/** Invoked by the terminal launcher through the host application's runCommand task. */
class AdminSetupCommand implements ApplicationCommand {

    AdminAccountService adminAccountService

    boolean handle(ExecutionContext context) {
        if (adminAccountService.configured()) {
            System.out.println('Grails Admin already has an account; setup is disabled.')
            return false
        }
        Console console = System.console()
        String username
        char[] password
        char[] confirmation
        boolean acceptedWarning = false
        if (console) {
            username = console.readLine('Admin username: ')
            password = console.readPassword('Password: ')
            confirmation = console.readPassword('Confirm password: ')
            if (password && (password.length < 12 ||
                StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(password)).remaining() > 72)) {
                System.out.println('Warning: passwords under 12 characters are easier to guess. Passwords over 72 UTF-8 bytes use SHA-256 before bcrypt so the full password is checked.')
                acceptedWarning = console.readLine('Use this password anyway? Type yes or no: ')?.trim()?.equalsIgnoreCase('yes')
                if (!acceptedWarning) {
                    System.out.println('Setup cancelled.')
                    Arrays.fill(password, '\u0000' as char)
                    if (confirmation) Arrays.fill(confirmation, '\u0000' as char)
                    return false
                }
                }
        } else if (System.getenv('GRAILS_ADMIN_SETUP_USERNAME') && System.getenv('GRAILS_ADMIN_SETUP_PASSWORD') &&
            System.getenv('GRAILS_ADMIN_SETUP_CONFIRMATION')) {
            username = System.getenv('GRAILS_ADMIN_SETUP_USERNAME')
            password = System.getenv('GRAILS_ADMIN_SETUP_PASSWORD').toCharArray()
            confirmation = System.getenv('GRAILS_ADMIN_SETUP_CONFIRMATION').toCharArray()
            acceptedWarning = System.getenv('GRAILS_ADMIN_SETUP_PASSWORD_WARNING_ACCEPTED') == 'yes'
        } else {
            System.err.println('Admin setup needs a terminal or the bundled secure setup launcher.')
            return false
            }
        try {
            adminAccountService.createInitialAccount(username, password, confirmation, acceptedWarning)
            System.out.println('Grails Admin account created.')
            true
        } catch (IllegalArgumentException | IllegalStateException error) {
            System.err.println(error.message)
            false
        } finally {
            if (password) Arrays.fill(password, '\u0000' as char)
            if (confirmation) Arrays.fill(confirmation, '\u0000' as char)
        }
    }

}
