package grailsadmin

import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AdminErrorFilter extends OncePerRequestFilter {
    private static final String PREFIX = '\u001B[1;31m[GRAILS-ADMIN ERROR]\u001B[0m'
    static final String ERROR_ID_ATTRIBUTE = 'grailsAdminErrorId'

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response)
        } catch (Exception error) {
            String errorId = UUID.randomUUID().toString()
            String path = (request.requestURI?.substring(request.contextPath?.length() ?: 0) ?: '/admin')
                .replaceAll(/[\r\n\u001B]/, ' ')
            System.err.println("${PREFIX} ${errorId} ${request.method} ${path}: unexpected failure")
            error.printStackTrace(System.err)
            System.err.flush()
            if (response.committed) throw error
            response.reset()
            request.setAttribute(ERROR_ID_ATTRIBUTE, errorId)
            try {
                request.getRequestDispatcher('/admin/error').forward(request, response)
            } catch (Exception renderingError) {
                if (response.committed) throw renderingError
                response.reset()
                response.status = 500
                response.contentType = 'text/html;charset=UTF-8'
                response.writer.write('<!doctype html><html><head><title>Grails Admin error</title></head>' +
                    '<body><h1>Grails Administration</h1><p>An unexpected error occurred.</p>' +
                    '<p>Error ID: ' + errorId + '</p><a href="/admin">Admin home</a></body></html>')
            }
        }
    }
}
