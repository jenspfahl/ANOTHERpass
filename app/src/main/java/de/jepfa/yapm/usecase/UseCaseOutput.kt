package de.jepfa.yapm.usecase

data class UseCaseOutput<OUTPUT>(val success: Boolean, val data: OUTPUT) {

    constructor(output: OUTPUT): this(true, output)

    companion object {
        fun ok(): UseCaseOutput<Unit> {
            return UseCaseOutput(true, Unit)
        }

        fun fail(): UseCaseOutput<Unit> {
            return UseCaseOutput(false, Unit)
        }

        fun of(success: Boolean): UseCaseOutput<Unit> {
            return UseCaseOutput(success, Unit)
        }
    }
}