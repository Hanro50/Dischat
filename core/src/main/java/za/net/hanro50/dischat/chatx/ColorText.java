package za.net.hanro50.dischat.chatx;

import java.awt.Color;

import com.google.gson.annotations.Expose;

public class ColorText extends PlainText {

  @Expose
  final public Color color;

  ColorText(String content, Color color) {
    super(content);
    this.color = color;
  }

}
