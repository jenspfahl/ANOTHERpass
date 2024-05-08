# ANOTHERpass

This Android app is another approach of a password manager. Instead of managing manually created passwords this app encourages to generate passwords. Passwords can be generated in a human readable but secure way by default, called Pseudo Phrases. Another feature is a two-factor authentication with knowledge (a user PIN) and ownership (a master password physically stored as NFC tag or QR code). 

One consistent idea of the app is to use QR codes and NFC tags as offline-storage for credentials and secrets. Printed QR codes and NFC tags can easily be stored at a protected analogue place. With that and the option to separate the encrypted vault and the encryption keys it is possible to implement your own specific security and backup strategy.

Further this app tries to not be just another offline password manager by focusing on making it easy *AND* safe to work with secure password credentials.

The motivation of developing this app was curiosity of how cryptography works, how to code in Kotlin and the personal need of an easy, lightweight but secure password manager. It is a one-man-project without any warranty and pretension of absolute professionality.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/de.jepfa.yapm/)

**Core features of the app**

>* easy two-factor authentication
>* no complicated master password to memorize
>* easy to read generated passwords
>* strong modern encryption (AES, Blowfish, Chacha20)
>* Autofill support
>* biometrics (e.g. fingerprint) for stored master password
>* no insecure clipboard function by default
>* display passwords over other apps to easily type them
>* no internet connection needed, all is offline
>* export and import credential vault
>* share and outsource single credentials
>* obfuscate passwords
>* expiring credentials with notification
>* self destruction mode
>* Dark Theme support ;-)


With version 2, the app is able to act as a credential server in a local network. With [this browser extension](https://github.com/jenspfahl/anotherpass-webext) you can fetch certain credentials directly from a browser on different devices (e.g. your laptop) .

For more information see [https://anotherpass.jepfa.de](https://anotherpass.jepfa.de)

You can download the latest release version [here](https://anotherpass.jepfa.de/download/).

You are also welcome to hunt bugs :-)
