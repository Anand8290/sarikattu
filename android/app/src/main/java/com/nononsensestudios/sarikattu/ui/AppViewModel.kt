package com.nononsensestudios.sarikattu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nononsensestudios.sarikattu.data.SplitRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * One activity-scoped ViewModel: runs repository calls so they survive rotation,
 * and reports progress and errors to whichever screen is showing.
 */
class AppViewModel(val repo: SplitRepository) : ViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() { _error.value = null }

    fun run(block: suspend SplitRepository.() -> Unit, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.block()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = friendlyMessage(e)
            } finally {
                _busy.value = false
            }
        }
    }

    private fun friendlyMessage(e: Exception): String {
        if (e is IOException || e::class.simpleName.orEmpty().contains("HttpRequest")) {
            return "Couldn't reach the server. Check your connection."
        }
        // Postgres exceptions raised in /supabase/migrations come back as the first line.
        val message = e.message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()
        return message?.take(160) ?: "Something went wrong. Try again."
    }
}
