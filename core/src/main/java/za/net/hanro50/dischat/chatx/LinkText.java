package za.net.hanro50.dischat.chatx;

import com.google.gson.annotations.Expose;

public class LinkText extends PlainText {
  @Expose
  final public String url;

  LinkText(String link) {
    super(link);
    this.url = link;
  }

  LinkText(String content, String link) {
    super(content);
    this.url = link;
  }
}
