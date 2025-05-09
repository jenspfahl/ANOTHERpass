package de.jepfa.yapm.viewmodel

import android.content.Context
import androidx.lifecycle.*
import de.jepfa.yapm.database.repository.LabelRepository
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.AutoBackupService
import de.jepfa.yapm.ui.YapmApp
import kotlinx.coroutines.launch

class LabelViewModel(private val repository: LabelRepository) : ViewModel() {

    val allLabels: LiveData<List<EncLabel>> = repository.getAll().asLiveData()

    fun getById(id: Int): LiveData<EncLabel> {
        return repository.getById(id).asLiveData()
    }

    fun insert(label: EncLabel, context: Context) = viewModelScope.launch {
        repository.insert(label)
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
        AutoBackupService.autoExportVault(context)
    }

    fun update(label: EncLabel, context: Context) = viewModelScope.launch {
        repository.update(label)
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
        AutoBackupService.autoExportVault(context)
    }

    fun delete(label: EncLabel, context: Context)  = viewModelScope.launch {
        repository.delete(label)
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
        AutoBackupService.autoExportVault(context)
    }
    fun deleteById(id: Int, context: Context)  = viewModelScope.launch {
        repository.deleteById(id)
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
        AutoBackupService.autoExportVault(context)
    }
    fun deleteByIds(ids: List<Int>, context: Context)  = viewModelScope.launch {
        repository.deleteByIds(ids)
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
        AutoBackupService.autoExportVault(context)
    }
}


class LabelViewModelFactory(private val app: YapmApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LabelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LabelViewModel(app.labelRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}