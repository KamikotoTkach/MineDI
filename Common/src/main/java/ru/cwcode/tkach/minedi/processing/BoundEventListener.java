package ru.cwcode.tkach.minedi.processing;

import revxrsal.asm.BoundMethodCaller;

public record BoundEventListener(Object bean, BoundMethodCaller caller) {
}
