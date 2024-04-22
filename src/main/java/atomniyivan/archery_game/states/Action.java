package atomniyivan.archery_game.states;

public record Action(Type type, String info) {
    public enum Type {
        NEW_PLAYER,
        STATE,
        WANT_TO_START,
        UPDATE,
        SHOOT,
        WANT_TO_PAUSE,
        WINNER,
        RESET,
        REMOVE_PLAYER,
        STOP,
    }
}
