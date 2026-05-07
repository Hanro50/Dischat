package za.net.hanro50.dischat.core.chatx;

import java.awt.Color;

import com.google.gson.annotations.Expose;

import za.net.hanro50.dischat.core.Chater;

public class Mention extends ColorText {
  @Expose
  final public Chater person;

  Mention(Chater person, Color color) {
    super(person.name, color);
    this.person = person;
  }

}
