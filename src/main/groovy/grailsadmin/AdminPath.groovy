package grailsadmin

class AdminPath {
    static String from(config) {
        String path = config.getProperty('grailsAdmin.basePath', String, '/admin')
        if (!(path ==~ /\/[A-Za-z0-9_-]+(?:\/[A-Za-z0-9_-]+)*/)) {
            throw new IllegalArgumentException('grailsAdmin.basePath must be an absolute path without a trailing slash')
        }
        path
    }
}
