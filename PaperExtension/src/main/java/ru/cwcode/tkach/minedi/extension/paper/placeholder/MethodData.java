package ru.cwcode.tkach.minedi.extension.paper.placeholder;

import revxrsal.asm.BoundMethodCaller;

public record MethodData(Class<?>[] parameters, BoundMethodCaller boundMethodCaller) {
}
