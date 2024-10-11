package de.jepfa.yapm.service.net

/**
 * None -> Incoming -> AwaitingAcceptance -> Accepted -> Fulfilled
 *                                        -> Denied
 */
enum class CredentialRequestState(val isProgressing: Boolean) {
    /**
     * No incoming request, ready to take one
     */
    None(false),
    /**
     * Incoming request, confirmation dialog will be displayed to the user (if configured)
     */
    Incoming(true),
    /**
     * Incoming request awaits confirmation from ddsplayed dialog
     */
    AwaitingAcceptance(true),
    /**
     * Incoming request accepted by the user or automatically (if configured)
     */
    Accepted(true),
    /**
     * Incoming request fulfilled by delivering requested data back
     */
    Fulfilled(false),
    /**
     * Incoming request declined and aborted
     */
    Denied(false);
}