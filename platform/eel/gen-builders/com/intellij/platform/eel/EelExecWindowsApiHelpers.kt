// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/**
 * This file is generated by [com.intellij.platform.eel.codegen.BuildersGeneratorTest].
 */
package com.intellij.platform.eel

import com.intellij.platform.eel.*
import com.intellij.platform.eel.EelExecApi.ExecuteProcessOptions
import com.intellij.platform.eel.EelExecApi.InteractionOptions
import com.intellij.platform.eel.EelExecApi.PtyOrStdErrSettings
import com.intellij.platform.eel.path.EelPath
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.ApiStatus


/**
 * @param exe An **absolute** path to the executable.
 *  TODO Or do relative paths also work?
 *
 *  All argument, all paths, should be valid for the remote machine. F.i., if the IDE runs on Windows, but IJent runs on Linux,
 *  [ExecuteProcessOptions.workingDirectory] is the path on the Linux host. There's no automatic path mapping in this interface.
 */
@GeneratedBuilder.Result
@ApiStatus.Experimental
fun EelExecWindowsApi.spawnProcess(
  exe: String,
): EelExecWindowsApiHelpers.SpawnProcess =
  EelExecWindowsApiHelpers.SpawnProcess(
    owner = this,
    exe = exe,
  )

@ApiStatus.Experimental
object EelExecWindowsApiHelpers {
  /**
   * Create it via [com.intellij.platform.eel.EelExecWindowsApi.spawnProcess].
   */
  @GeneratedBuilder.Result
  @ApiStatus.Experimental
  class SpawnProcess(
    private val owner: EelExecWindowsApi,
    private var exe: String,
  ) : OwnedBuilder<EelWindowsProcess> {
    private var args: List<String> = listOf()

    private var env: Map<String, String> = mapOf()

    private var interactionOptions: InteractionOptions? = null

    private var ptyOrStdErrSettings: PtyOrStdErrSettings? = interactionOptions

    private var scope: CoroutineScope? = null

    private var workingDirectory: EelPath? = null

    @ApiStatus.Experimental
    fun args(arg: List<String>): SpawnProcess = apply {
      this.args = arg
    }

    fun args(vararg arg: String): SpawnProcess = apply {
      this.args = listOf(*arg)
    }

    /**
     * By default, environment is always inherited, which may be unwanted. [ExecuteProcessOptions.env] allows
     * to alter some environment variables, it doesn't clear the variables from the parent. When the process should be started in an
     * environment like in a terminal, the response of [fetchLoginShellEnvVariables] should be put into [ExecuteProcessOptions.env].
     */
    @ApiStatus.Experimental
    fun env(arg: Map<String, String>): SpawnProcess = apply {
      this.env = arg
    }

    /**
     * An **absolute** path to the executable.
     * TODO Or do relative paths also work?
     *
     * All argument, all paths, should be valid for the remote machine. F.i., if the IDE runs on Windows, but IJent runs on Linux,
     * [ExecuteProcessOptions.workingDirectory] is the path on the Linux host. There's no automatic path mapping in this interface.
     */
    @ApiStatus.Experimental
    fun exe(arg: String): SpawnProcess = apply {
      this.exe = arg
    }

    /**
     * When set pty, be sure to accept esc codes for a terminal you are emulating.
     * This terminal should also be set in `TERM` environment variable, so setting it in [env] worth doing.
     * If not set, `xterm` will be used as a most popular one.
     *
     * See `termcap(2)`, `terminfo(2)`, `ncurses(3X)` and ISBN `0937175226`.
     */
    @ApiStatus.Experimental
    fun interactionOptions(arg: InteractionOptions?): SpawnProcess = apply {
      this.interactionOptions = arg
    }

    @Deprecated("Switch to interactionOptions", replaceWith = ReplaceWith("interactionOptions"))
    @ApiStatus.Internal
    fun ptyOrStdErrSettings(arg: PtyOrStdErrSettings?): SpawnProcess = apply {
      this.ptyOrStdErrSettings = arg
    }

    /**
     * Scope this process is bound to. Once scope dies -- this process dies as well.
     */
    fun scope(arg: CoroutineScope?): SpawnProcess = apply {
      this.scope = arg
    }

    /**
     * All argument, all paths, should be valid for the remote machine. F.i., if the IDE runs on Windows, but IJent runs on Linux,
     * [ExecuteProcessOptions.workingDirectory] is the path on the Linux host. There's no automatic path mapping in this interface.
     */
    @ApiStatus.Experimental
    fun workingDirectory(arg: EelPath?): SpawnProcess = apply {
      this.workingDirectory = arg
    }

    /**
     * Complete the builder and call [com.intellij.platform.eel.EelExecWindowsApi.spawnProcess]
     * with an instance of [com.intellij.platform.eel.EelExecApi.ExecuteProcessOptions].
     */
    @ThrowsChecked(ExecuteProcessException::class)
    override suspend fun eelIt(): EelWindowsProcess =
      owner.spawnProcess(
        ExecuteProcessOptionsImpl(
          args = args,
          env = env,
          exe = exe,
          interactionOptions = interactionOptions,
          ptyOrStdErrSettings = ptyOrStdErrSettings,
          scope = scope,
          workingDirectory = workingDirectory,
        )
      )
  }
}