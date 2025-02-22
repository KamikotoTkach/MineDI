package ru.cwcode.tkach.minedi.condition;

public interface Condition {
  String name();
  boolean process(String value);
}
