package za.net.hanro50.dischat.core;

import java.util.Collection;

public interface ChatConsumer {

  public static class Link implements Comparable<Link> {
    public String link;
    public String name;
    public int id;

    @Override
    public int compareTo(Link l) {
      return l.id - this.id;
    }
  }

  public void accept(Chater chater, String content, Collection<Link> link);
}
