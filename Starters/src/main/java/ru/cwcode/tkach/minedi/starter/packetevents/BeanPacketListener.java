package ru.cwcode.tkach.minedi.starter.packetevents;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import ru.cwcode.tkach.minedi.DiApplication;

import java.util.ArrayList;
import java.util.List;

final class BeanPacketListener implements PacketListener {
  private final DiApplication application;
  private final List<PacketHandler> receivers = new ArrayList<>();
  private final List<PacketHandler> senders = new ArrayList<>();

  BeanPacketListener(DiApplication application) {
    this.application = application;
  }

  List<PacketHandler> receivers() {
    return receivers;
  }

  List<PacketHandler> senders() {
    return senders;
  }

  @Override
  public void onPacketReceive(PacketReceiveEvent event) {
    dispatch(receivers, event);
  }

  @Override
  public void onPacketSend(PacketSendEvent event) {
    dispatch(senders, event);
  }

  private void dispatch(List<PacketHandler> handlers, ProtocolPacketEvent event) {
    for (PacketHandler handler : handlers) {
      if (!handler.accepts(event.getPacketType())) continue;

      try {
        handler.call(event);
      } catch (Throwable e) {
        application.getLogger().warn("{} failed on packet {}", handler, event.getPacketType(), e);
      }
    }
  }
}
