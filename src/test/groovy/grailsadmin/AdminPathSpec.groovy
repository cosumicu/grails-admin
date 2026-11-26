package grailsadmin

import org.grails.config.PropertySourcesConfig
import spock.lang.Specification

class AdminPathSpec extends Specification {
    void 'defaults to admin and accepts a custom absolute path'() {
        expect:
        AdminPath.from(new PropertySourcesConfig()) == '/admin'
        AdminPath.from(new PropertySourcesConfig([grailsAdmin: [basePath: '/backoffice/team_1']])) == '/backoffice/team_1'
    }

    void 'rejects unsafe or malformed paths'() {
        when:
        AdminPath.from(new PropertySourcesConfig([grailsAdmin: [basePath: path]]))

        then:
        thrown(IllegalArgumentException)

        where:
        path << ['', '/', 'backoffice', '/backoffice/', '/backoffice//team', '/backoffice?next=evil', '//evil']
    }
}
