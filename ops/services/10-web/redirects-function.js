const redirects = {
    '/overview': '/',
    '/accessing-claims-data': '/production-access',
    '/understanding-ab2d-data': '/ab2d-data',
    '/tutorial-postman-swagger': '/api-documentation',
    '/tutorial-curl': '/api-documentation',
    '/advanced-user-guide': '/api-documentation',
    '/data_dictionary': '/ab2d-data',
    '/setup-mac': '/setup-instructions',
    '/setup-linux': '/setup-instructions',
    '/setup-windows': '/setup-instructions'
}

async function handler(event) {
    const request = event.request;

    // Handle Site Redirects (ex. https://github.com/aws-samples/amazon-cloudfront-functions/blob/main/redirect-based-on-country/index.js)
    let uri = request.uri
    if (uri.endsWith('.html')) {
        uri = uri.slice(0, -5);
    }

    if (redirects[uri]) {
        const response = {
            statusCode: 301,
            statusDescription: 'Moved Permanently',
            headers: {
                location: {
                    value: 'https://' + request.headers.host.value + redirects[uri]
                }
            }
        }
        return response;
    }

    // Handle "Cool URIs" (ex. https://github.com/aws-samples/amazon-cloudfront-functions/blob/main/url-rewrite-single-page-apps/index.js)
    // Rewrite to index.html in a directory when requesting its root
    if (request.uri.endsWith('/')) {
        request.uri += 'index.html';
    }
    // Rewrite with added ".html" when there's no file extension
    else if (!request.uri.includes('.')) {
        request.uri += '.html';
    }

    return request
}
