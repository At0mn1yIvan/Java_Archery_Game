package atomniyivan.archery_game.models;

import java.util.ArrayList;
import java.util.List;

public class PlayMapInfo {
    public final TargetModel bigTarget;
    public final TargetModel smallTarget;
    public final List<Player> playersList = new ArrayList<>();

    public PlayMapInfo(final double height) {
        bigTarget = new TargetModel(226.0, 0.5 * height, 44.0, 3);
        smallTarget = new TargetModel(311.0, 0.5 * height, 22.0, 6);
    }
}
