# ANOTHERpass

This Android app is another way to manage passwords. It addresses the problem of having to remember a strong master password, which is more often a weak one because it is created by humans.

To avoid weak master passwords, the app generates them! The app requires two factors to unlock the vault, the generated master password (ownership in the sense of a key) and a PIN (knowledge, in the sense of a remembered secret).

One consistent idea of the app is to use QR codes and NFC tags as offline-storage for credentials and secrets, such as the master password. Printed QR codes and NFC tags can be easily stored in a protected analogue place. With this and the ability to separate the encrypted vault and the encryption key, it is possible to implement your own specific security and backup strategy.

Instead of managing manually created passwords, this app encourages using only generated passwords. By default, passwords can be generated in a human-readable but secure way, called Pseudo Phrases. 

Additionally, this app tries to avoid being just another offline password manager by focusing on making working with strong passwords easy *AND* safe.

The motivation of developing this app was curiosity of how cryptography works, how to code in Kotlin and the personal need of an easy, lightweight but secure password manager. It is a one-man-project without any warranty and pretension of absolute professionality.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/de.jepfa.yapm/)


## Screenshots
<details>
<summary>Show screenshots</summary>

![](metadata/en-US/images/phoneScreenshots/1.png)
![](metadata/en-US/images/phoneScreenshots/3.png)
![](metadata/en-US/images/phoneScreenshots/4.png)
![](metadata/en-US/images/phoneScreenshots/5.png)
![](metadata/en-US/images/phoneScreenshots/6.png)
![](metadata/en-US/images/phoneScreenshots/7.png)

Dark theme:
![](metadata/en-US/images/phoneScreenshots/A.png)
![](metadata/en-US/images/phoneScreenshots/B.png)
![](metadata/en-US/images/phoneScreenshots/C.png)
![](metadata/en-US/images/phoneScreenshots/D.png)

</details>

## Core features of the app

>* easy two-factor authentication
>* no complicated master password to remember
>* easy to read generated passwords
>* strong modern encryption with 128 or 256 bit keys (AES, Blowfish, Chacha20)
>* Autofill support
>* biometrics (e.g. fingerprint) for stored master password
>* no insecure clipboard function by default
>* display passwords over other apps to easily type them
>* no internet connection needed, all is offline
>* export and import credential vault
>* share and outsource single credentials
>* one-time-password support (HOTP and TOTP)
>* obfuscate passwords
>* expiring credentials with notification
>* self destruction mode
>* Dark Theme support ;-)


With version 2, the app is able to act as a credential server in a local network. With [this browser extension](https://github.com/jenspfahl/anotherpass-browserextension) you can fetch certain credentials directly from a browser on different devices (e.g. your laptop) .

For more information see [https://anotherpass.jepfa.de](https://anotherpass.jepfa.de)

Information about the ANOTHERpass browser extension can be found [here](https://anotherpass.jepfa.de/extension/).

You can download the latest release version [here](https://anotherpass.jepfa.de/download/).

You are also welcome to hunt bugs :-)
