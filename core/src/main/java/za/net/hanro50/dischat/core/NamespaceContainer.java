package za.net.hanro50.dischat.core;

public class NamespaceContainer {
  public String origin;
  public String path;

  public NamespaceContainer(String origin, String path) {
    this.origin = origin;
    this.path = path;
  }

  public static NamespaceContainer literal(String name) {
    return new NamespaceContainer("minecraft", name);
  }
}
