package com.yourname.teleprompter.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.teleprompter.data.local.ScriptDao
import com.yourname.teleprompter.data.local.entity.ScriptEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val dao: ScriptDao
) : ViewModel() {

    private val _script = MutableStateFlow<ScriptEntity?>(null)
    val script: StateFlow<ScriptEntity?> = _script.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            _script.value = dao.getById(id)
        }
    }

    fun save(entity: ScriptEntity) {
        viewModelScope.launch {
            dao.upsert(entity.copy(updatedAt = System.currentTimeMillis()))
        }
    }
}