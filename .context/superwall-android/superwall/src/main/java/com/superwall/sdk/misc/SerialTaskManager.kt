package com.superwall.sdk.misc

import kotlinx.coroutines.*
import java.util.*
import java.util.LinkedList
import java.util.Queue

class SerialTaskManager(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1)),
) {
    private val taskQueue: Queue<suspend () -> Unit> = LinkedList()
    private var currentTask: Deferred<Unit>? = null

    fun addTask(task: suspend () -> Unit) {
        coroutineScope.launch {
            // Wait for the current task to finish if there is one
            currentTask?.await()

            // Add the task to the queue
            taskQueue.offer(task)

            // If there's only one task in the queue, start executing it
            if (taskQueue.size == 1) {
                executeNextTask()
            }
        }
    }

    private suspend fun executeNextTask() {
        // Check if there are tasks in the queue
        if (taskQueue.isEmpty()) {
            return
        }

        // Get the next task from the queue
        val nextTask = taskQueue.poll() ?: return

        // Run the task and wait for it to complete
        currentTask =
            coroutineScope.async {
                nextTask()
            }
        currentTask?.await()

        // After the task completes, recursively execute the next task
        if (taskQueue.isNotEmpty()) {
            executeNextTask()
        }
    }
}
