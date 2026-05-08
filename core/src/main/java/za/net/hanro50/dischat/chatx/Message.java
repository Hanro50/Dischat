package za.net.hanro50.dischat.chatx;

import java.util.List;

import com.google.gson.annotations.Expose;

import za.net.hanro50.dischat.objects.Chater;

public class Message {
  @Expose
  final public List<PlainText> elements;
  @Expose
  final public Chater sender;

  Message(Chater chater, List<PlainText> elements) {
    this.sender = chater;
    this.elements = elements;
  }

}
