package za.net.hanro50.dischat.objects;

public interface InfoProvider {

  public static class Result {
    public int maxPlayers;
    public int onlinePlayerCount;
    public double tps;
    public byte[] icon;
  }

  public Result accept();
}
