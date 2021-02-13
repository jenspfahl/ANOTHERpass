package de.jepfa.yapm.viewmodel

import androidx.lifecycle.*
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.repository.CredentialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CredentialViewModel(private val repository: CredentialRepository) : ViewModel() {

    // Using LiveData and caching what allWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allWords: LiveData<List<EncCredential>> = repository.getAll().asLiveData()

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(credential: EncCredential) = viewModelScope.launch {
        repository.insert(credential)
    }
}


class CredentialViewModelFactory(private val repository: CredentialRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CredentialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CredentialViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}