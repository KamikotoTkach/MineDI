package ru.cwcode.minediprobe.paper;

import ru.cwcode.tkach.minedi.annotation.Service;

@Service
public class GreetingService {
  ProbeConfig config;

  public int answer() {
    return config.answer;
  }
}
