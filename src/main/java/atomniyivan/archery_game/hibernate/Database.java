package atomniyivan.archery_game.hibernate;

import atomniyivan.archery_game.models.Player;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.LockModeType;
import java.util.List;


public class Database {

    public static void updateDataDb(Player p) {
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            Player existingPlayer = (Player) session.createQuery("FROM Player WHERE username = :username")
                    .setParameter("username", p.username)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .uniqueResult();

            if (existingPlayer != null) {
                existingPlayer.wins++;
                session.update(existingPlayer);
            } else {
                tx.commit();
                session.close();

                session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();

                p.wins++;
                session.save(p);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            if (session != null) session.close();
        }
    }

    public static List<Player> getLeaderboardFromDb(){
        List<Player> leaderboard = null;
        Session session = null;
        try {
            session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
            leaderboard = session.createQuery("FROM Player ORDER BY wins DESC", Player.class).list();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return leaderboard;
    }
}
