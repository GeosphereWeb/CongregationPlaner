package de.geosphere.congregationplaner.testing

import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.test.assertEquals

/** Liefert einen Task, der seine Listener sofort mit [value] bedient. */
internal inline fun <reified T> successTask(value: T): Task<T> {
    val task = mockk<Task<T>>()
    every { task.addOnSuccessListener(any<OnSuccessListener<in T>>()) } answers {
        firstArg<OnSuccessListener<in T>>().onSuccess(value)
        task
    }
    every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
    return task
}

/** Liefert einen Task, der seine Failure-Listener sofort mit [exception] bedient. */
internal inline fun <reified T> failedTask(exception: Exception): Task<T> {
    val task = mockk<Task<T>>()
    every { task.addOnSuccessListener(any<OnSuccessListener<in T>>()) } returns task
    every { task.addOnFailureListener(any<OnFailureListener>()) } answers {
        firstArg<OnFailureListener>().onFailure(exception)
        task
    }
    return task
}

/** Führt [block] mit gemocktem `android.util.Log` aus (in Host-Tests nicht verfügbar). */
internal inline fun <T> withMockedLog(block: () -> T): T {
    mockkStatic(Log::class)
    every { Log.e(any(), any(), any<Throwable>()) } returns 0
    return try {
        block()
    } finally {
        unmockkStatic(Log::class)
    }
}

/**
 * Prüft, dass [result] mit einer Exception vom Typ und mit der Nachricht von [expected] fehlschlägt.
 * Identität wird bewusst nicht geprüft, da Coroutines Exceptions beim Weiterreichen kopieren können.
 */
internal fun assertFailure(expected: Throwable, result: Result<*>) {
    val actual = result.exceptionOrNull()
    assertEquals(expected::class, actual?.let { it::class })
    assertEquals(expected.message, actual?.message)
}
