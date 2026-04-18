package at.flauschigesalex.maintenance.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val job = SupervisorJob() + Dispatchers.IO
internal val asyncScope = CoroutineScope(job)

internal fun scheduleAsync(block: suspend () -> Unit) {
    asyncScope.launch { block() }
}
