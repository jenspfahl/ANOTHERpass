package de.jepfa.yapm.usecase

data class UseCaseOutput<OUTPUT>(val success: Boolean, val data: OUTPUT, var errorMessage: String?) {

    constructor(output: OUTPUT): this(true, output, null)
    constructor(success: Boolean, output: OUTPUT): this(success, output, null)

    companion object {
        fun ok(): UseCaseOutput<Unit> {
            return UseCaseOutput(true, Unit, null)
        }

        fun <T> fail(errorMessage: String?): UseCaseOutput<T?> {
            return UseCaseOutput(false, null, errorMessage)
        }

        fun of(success: Boolean, errorMessage: String?): UseCaseOutput<Unit> {
            return UseCaseOutput(success, Unit, errorMessage)
        }
    }
}