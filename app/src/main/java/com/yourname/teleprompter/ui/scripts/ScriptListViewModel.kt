package com.yourname.teleprompter.ui.scripts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.teleprompter.data.local.ScriptDao
import com.yourname.teleprompter.data.local.entity.ScriptEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ScriptListViewModel @Inject constructor(
    private val dao: ScriptDao
) : ViewModel() {

    val scripts: StateFlow<List<ScriptEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createNew(): String {
        val id = UUID.randomUUID().toString()
        viewModelScope.launch {
            dao.upsert(ScriptEntity(
                id = id,
                title = "新文稿",
                content = "",
                updatedAt = System.currentTimeMillis()
            ))
        }
        return id
    }

    fun delete(id: String) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}