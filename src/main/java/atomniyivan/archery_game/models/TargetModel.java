package atomniyivan.archery_game.models;

public class TargetModel {
    public double x;
    public double y;
    public final double radius;
    public final double speed;
    public int direction = 1;

    public TargetModel(double x, double y, double radius, double speed) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.speed = speed;
    }
}
