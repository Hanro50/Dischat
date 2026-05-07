package za.net.hanro50.dischat.core.chatx;

import com.google.gson.annotations.Expose;

public class LinkText extends PlainText {
  @Expose
  final public String url;
  @Expose
  final public boolean sticker;

  LinkText(String link, boolean isSticker) {
    super(link);
    this.url = link;
    this.sticker = isSticker;
  }

  LinkText(String content, String link, boolean isSticker) {
    super(content);
    this.url = link;
    this.sticker = isSticker;
  }
}
