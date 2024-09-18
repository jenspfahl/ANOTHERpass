package de.jepfa.yapm.service.net

//TODO rename to RequestCredentialCommand
enum class FetchCredentialCommand(val command: String) {
    CREATE_CREDENTIAL_FOR_URL("create_credential_for_url"),
    FETCH_CREDENTIAL_FOR_URL("fetch_credential_for_url"),
    FETCH_CREDENTIAL_FOR_UID("fetch_credential_for_uid"),
    FETCH_CREDENTIALS_FOR_UIDS("fetch_credentials_for_uids"),
    FETCH_SINGLE_CREDENTIAL("fetch_single_credential"),
    FETCH_MULTIPLE_CREDENTIALS("fetch_multiple_credentials"),
    FETCH_ALL_CREDENTIALS("fetch_all_credentials"),
    FETCH_CLIENT_KEY("get_client_key"),
    CANCEL_REQUEST("cancel_request"),
    ;

    companion object {
        fun getByCommand(command: String): FetchCredentialCommand {
            return values().first { it.command == command }
        }
    }

}
