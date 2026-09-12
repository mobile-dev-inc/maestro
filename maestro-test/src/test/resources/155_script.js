// BASE_URL is a flow env var; MAESTRO_JS_HTTP_TIMEOUT comes from this command's own env
// block. Together they prove the env default reaches the http binding through Orchestra
// and runScript's sub-scope, rather than being set on the engine directly by a test.
http.post(BASE_URL + '/slow', {
    body: JSON.stringify({ payload: 'Value' })
})
