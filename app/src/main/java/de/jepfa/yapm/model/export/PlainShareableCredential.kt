package de.jepfa.yapm.model.export

import de.jepfa.yapm.model.secret.Password


data class PlainShareableCredential(val n: String,
                                    val aI: String,
                                    val u: String,
                                    val p: Password,
                                    val w: String,
)

