# ANOTHERpass built-in server API specification

From [ANOTHERpass version 2](https://github.com/jenspfahl/ANOTHERpass) onwards, a new server capability is included in the mobile app, to let the app function as a credential server in a local network. Together with the [ANOTHERpass Browser Extension](https://github.com/jenspfahl/anotherpass-browserextension) it is possible to easily and securely use credentials stored in the app by any computer in a local network. This is a very requested feature to use credentials outside of the mobile device.

This server capability of the app can be used by any client, who follows this API. This document is about describing this API.

## Limitations

The server capability is designed to serve in local networks only, never as a public server visible from the public. Therefore no TLS and certificates is used to secure the connection. Instead an end-to-end encryption is established, [see this section](https://github.com/jenspfahl/anotherpass-browserextension?tab=readme-ov-file#common-communication-between-extension-and-app).


## Transport layer

* Communication over HTTP only
    * The client has to implement the service layer described in this document and in [this document](https://github.com/jenspfahl/anotherpass-browserextension?tab=readme-ov-file#common-communication-between-extension-and-app).
* Supported HTTP methods:
    * `OPTIONS` : to tell the client about the supported HTTP methods and headers and application content
    * `POST` : to send requests to the server
    * `GET` : to download the vault backup file


### HTTP headers

* `Accept`: "application/json"
* `Content-Type`: "application/json"
* `X-WebClientId`: \<webClientId\>

`X-WebClientId` is the only custom header used by the server to identify the calling client. This identifier is generated once by the client during [the linking phase](https://github.com/jenspfahl/anotherpass-browserextension?tab=readme-ov-file#link-extension-with-the-app) and must be a random and unique 6 alphanumeric uppercase char sequence in the format `[A-Z]{3}-[A-Z]{3}` (e.g. `ABC-DEF`). It is the only value transferred in plaintext.

### HTTP routes
The server replies only to the root route ("/").
Other routes return HTTP error code 404.


### Request body structure

Every request has the same basic structure (JSON format):

```
{ 
    "encOneTimeKey": <base64 encoded and RSA-encrypted Request Transport Key or Session Key for linking phase>
    "envelope": <base64 encoded and AES-encrypted request payload>
}
```

### Response body structure

The response body looks similar to the request body. A body is only contained if the HTTP code is 200.

```
{ 
    "encOneTimeKey": <base64 encoded and RSA-encrypted Response Transport Key or Session Key for linking phase>
    "envelope": <base64 encoded and AES-encrypted request payload>
}
```

### HTTP status codes

 * 200 success
 * 202 accepted, but continue requesting
 * 204 success, but no content
 * 400 malformed or bad client request
 * 401 app vault is locked
 * 403 request denied by the user, client should stop current request
 * 404 webClientId unknown or no user interaction
 * 409 conflicting request, current request is rejected until the concurrent request is proceeded
 * 500 error in the server/app

Clients can keep polling if they receive one of those status codes:
 * 202 --> user should accept the incoming request or perform the requested action
 * 401 --> user should open the app and unlock the vault

Clients should stop polling if they receive:
 * 200 --> the request is fulfilled and the requested data should be in the response payload
 * 204 --> the request was fulfilled with the previous request
 * 403 --> the user has denied the request in the app

Clients should stop polling due to an error:
 * 400 --> the client hasn't correctly implemented against this API spec
 * 404 --> due to a misconfiguration, related data cannot be found on the server or the app and the client are not linked
 * 500 --> an error in the app/server, please report an issue in Github

### Request types and payload structure

Every request contains an encrypted `"envelope"` with a payload. Each plain request payload has an `"action"` property in common, the rest can differ, depending on the action.

```
{
    "action": <a string telling the server what to do>,
    ...
}
```

#### Supported actions

The follow strings are supported:

 * `link_app`: request the client to be linked with the app
 * `request_credential`: any further requests to obtain credentials from and intact with the app

##### `link_app`- Linking request and response

A linking request looks like this:

```
{
    "action": "link_app",
    "clientPubKey": {
        "n" : <modulus as base64 of the Public Key of the client>,
        "e" : <exponent as base64 of the Public Key of the client>
    }  
    "vaultId": <vaultId of the current linked app, only for relinking; absent in initial linking phase>
}
```

And the response like this:

```
{
    "linkedVaultId" : <string, the vault id of the app vault>,
    "sharedBaseKey": <base64 encoded Base Key, either 128 or 256 bit long depending on the mobile device>,	
    "serverPubKey": {
        "n" : <modulus as base64 of the Public Key of the app>,
        "e" : <exponent as base64 of the Public Key of the app>
    }
}
```


##### `request_credential` - Credential request and response

As request looks like this:

```
{
    "action": "request_credential",
    "command": <a command string to specify how the app should behave>,
    "requestIdentifier": <a unique random identifier per request, hashed with the Base Key by the app and presented in the app for comparison and acceptance as shortened fingerprint (see below)>
    ...
}
```

A successful response has this pattern:

```
{
    <command specific data>,
    "clientKey": <the base64 encoded AES key to unlock a client vault, see section ClientKey below>	
}
```

#### Supported commands

All commands initiate a different flow in the app and may enforce user interaction with the app. 

* `fetch_credential_for_url`

Requires `"website"` as a sibling of the `"command"` - property and asks the user of the app to select a credential for the given website. The app starts a credential search with the website domain name to support the user. User has to select one credential.

Returns the selected credential.

```
{
    "credential": {
        "uid": <the UUID of the credental stored in the app>,
        "readableUid": <the shortened and readable UUID of the credential>,
        "name": <the name of the credential stored in the app>,
        "password", <the raw password of the credential in plaintext>,
        "user": <the user of the credential if present>,
        "website": <the website information of the credential if present>,
        "opt": <an OTP configuration if present>
    }
    ..
}
```

* `fetch_credential_for_uid`

Requires `"uid"` as a sibling of the `"command"`- property and tries to fetch the credential with the given UID. No user interaction required.

Returns the related credential as for `fetch_credential_for_url` or 404 if no credential found for this UID.

* `fetch_credentials_for_uids`

Requires `"uids"`-array as a sibling of the `"command"` - property containing all UIDs to fetch. The app tries to fetch all credentials with the given UIDs. No user interaction required.

Returns all related credentials or 404 if no credential found for this UID.

```
{
    "credentials": [
        {
            <credential object, see above>
        },
        ..
    }
    ..
}
```

* `fetch_single_credential`

Asks the user of the app to select any credential to be fetched by the client. 

Returns the selected credential as for `fetch_credential_for_url` or 409 if nothing is selected.


* `fetch_multiple_credentials`

Asks the user of the app to select multiple credentials to be fetched by the client. For that, the app goes into multi-selection-mode.

Returns the selected credentials as for `fetch_credentials_for_uids` or 409 if nothing is selected.

* `fetch_all_credentials`

Tries to fetch all (but only unveiled) credentials. No user interaction required.

Returns all credentials as for `fetch_credentials_for_uids`.


* `create_credential_for_url`

Requires `"website"` as a sibling of the `"command"` - property and starts the flow to create a new credential with a suggested name,  and website derived from the received `"website"`. It may also receive a `"user"` from the client to be prefilled in the form.

Returns the created credential as for `fetch_credential_for_url` or 409 if nothing is created.


* `get_client_key`

Tries to derive the client key from the app's master key. No user interaction needed.

Returns the clientKey and no other credential.

* `cancel_request`

Cancels the current active request on the app. Executed when the user cancels the request in the client.

Returns no data.

* `download_vault_backup`

Initiates a download of a backup file of the app vault. Returns the filename and the download path used to eventually download the file with HTTP GET under a one-time path.
```
{
    "filename": "anotherpassvault_xxx.json",
    "downloadKey": "XXX-YYY_zzzzzzzzzzzzzzzzzzzzzzzzzzz" 
    ..
}
```

The file can now be downloaded once under http://\<server\>:\<port\>/\<downloadKey\>.

Note! This download is not encrypted by the configured end-to-end-encryption, because of restrictions of the Browser Extension JavaScript Download API used by possible clients like the browser extension. The reason is that a client can not manipulate (like encrypt) a download stream. Usually the backup file is encrypted but contains plain metadata which could be revealed in an untrusted local network.


#### Client Key for client based encryption

If the client wants to store received credentials, it can take advantage of the clientKey, which is an 128/256 bit long key derived from the apps master key. The clientKey is derived by taking the Webclient-Id, the master key and the vault salt and hashing all together (SHA-256). With that, a key is derived that cannot leak any information about the origin master key, but requires an interaction with the mobile app app to get. **This derived key should only be held in memory of the client, ideally with a TTL!** With that, a local encrypted credential storage / vault can be implemented on client side with having the app as the key factor.


### Request fingerprinting

Shortened fingerprint is a function that takes the first 7 alphanumeric characters of a base64 and converts them into an easy and fast to read fingerprint string in the format `[A-Z]{2}-[A-Z]{3}-[A-Z]{2}`.

The fingerprint is derived from:
 * the `"serverPubKey"."n"` - value for the "link_app" requests
 * or the Base Key and the `"requestIdentifier"` - value (see above), both joined and hashed (`SHA-256(BK + requestIdentifier)`) for the "request_credential" requests.

The output is encoded as Base64 and the first 7 alphabetic characters are printed out as uppercase in the format `AB-CDE-FG`.

The same fingerprint shall be calculated on the client side, so the user has the chance to compare both for equality to ensure the validity of the request.




