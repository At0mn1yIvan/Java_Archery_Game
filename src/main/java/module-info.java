open module atomniyivan.archery_game {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires com.google.gson;
    requires java.persistence;
    requires org.hibernate.orm.core;
    requires java.naming;
    requires java.sql;

    exports atomniyivan.archery_game.server;
    exports atomniyivan.archery_game.client;
    exports atomniyivan.archery_game.states;
    exports atomniyivan.archery_game.models;
    exports atomniyivan.archery_game.hibernate;
}