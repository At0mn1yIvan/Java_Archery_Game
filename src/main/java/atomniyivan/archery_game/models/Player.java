package atomniyivan.archery_game.models;

import javax.persistence.*;

@Entity
@Table (name = "winners")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name = "username")
    public String username;
    @Column(name = "wins")
    public int wins;
    @Transient
    public String color;
    @Transient
    public int score;
    @Transient
    public int shots;
    @Transient
    public ArrowInfo arrow = new ArrowInfo();
    @Transient
    public boolean wantToPause = false;
    @Transient
    public boolean wantToStart = false;
    @Transient
    public boolean isShooting = false;

    public Player(){}

    public Player(String name, String color) {
        this.username = name;
        this.color = color;
        this.wins = 0;
    }
}
