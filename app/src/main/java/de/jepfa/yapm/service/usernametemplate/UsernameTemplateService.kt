package de.jepfa.yapm.service.usernametemplate

import de.jepfa.yapm.model.encrypted.EncUsernameTemplate
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secretgenerator.SecretStrength
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGeneratorSpec

object UsernameTemplateService {

    private val passphraseGenerator = PassphraseGenerator(context = null)

    fun getUsernamesWithGeneratedAliases(key: SecretKeyHolder, encUsernameTemplates: List<EncUsernameTemplate>, credentialName: String?): List<String> {
        val list = HashSet<String>(encUsernameTemplates.size * 2)
        for (template in encUsernameTemplates) {
            val username = SecretService.decryptCommonString(key, template.username)
            val typeIdx = SecretService.decryptLong(key, template.generatorType) ?: 0
            val type = EncUsernameTemplate.GeneratorType.values()[typeIdx.toInt()]
            list.add(username)
            if (type == EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_CREDENTIAL_NAME_BASED || type == EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_BOTH) {
                list.add(nameBased(username, credentialName ?: ""))
            }
            if (type == EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_RANDOM_BASED || type == EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_BOTH) {
                list.add(randomBased(username))
            }

        }
        return list.sorted()
    }

    private fun randomBased(username: String): String {
        val passwd = passphraseGenerator.generate(PassphraseGeneratorSpec(strength = SecretStrength.ONE_WORD))
        return insertAlias(username, passwd.toRawFormattedPassword())
    }

    private fun nameBased(username: String, credentialName: String): String {
        val alias = credentialName.substringBefore(" ").lowercase()
        return insertAlias(username, alias)
    }

    private fun insertAlias(username: String, alias: CharSequence): String {
        val splitted = username.split('@')
        if (splitted.size != 2) {
            return username
        }
        val shortenedAlias = if (alias.length > 6) alias.substring(0, 6) else alias

        return "${splitted[0]}+$shortenedAlias@${splitted[1]}"
    }
}