/**
 * Velocity API is not on a Paper server: loading anything here would throw NoClassDefFoundError.
 */
@Conditional("hasPlugin(NoSuchPlugin)")
package ru.cwcode.minediprobe.paper.missing;

import ru.cwcode.tkach.minedi.annotation.Conditional;
