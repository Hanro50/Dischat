package za.net.hanro50.dischat.chatx;

import com.google.gson.annotations.Expose;

public class PlainText {
  @Expose
  final public String content;

  PlainText(String content) {
    this.content = content;
  }
}
