package za.net.hanro50.dischat.core;

public interface InfoProvider {

  public static class Result {
    public int maxPlayers;
    public int onlinePlayerCount;
    public float tps;
    public byte[] icon;
  }

  public Result accept();
}
