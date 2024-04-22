package atomniyivan.archery_game.models;

import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

public class ArrowModel extends Path {
    public ArrowModel(double startX, double startY, double endX, double endY, double arrowHeadSize, String playerColor) {
        super();
        strokeProperty().bind(fillProperty());
        setFill(Color.valueOf(playerColor));
        //Рисуем линию
        getElements().add(new MoveTo(startX, startY));
        getElements().add(new LineTo(endX, endY));
        //Наконечник
        double angle = Math.atan2((endY - startY), (endX - startX)) - Math.PI / 2.0;
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        //Первая точка наконечника стрелы
        double x1 = (-1.0 / 2.0 * cos + Math.sqrt(3) / 2 * sin) * arrowHeadSize + endX;
        double y1 = (-1.0 / 2.0 * sin - Math.sqrt(3) / 2 * cos) * arrowHeadSize + endY;
        //Вторая точка наконечника стрелы
        double x2 = (1.0 / 2.0 * cos + Math.sqrt(3) / 2 * sin) * arrowHeadSize + endX;
        double y2 = (1.0 / 2.0 * sin - Math.sqrt(3) / 2 * cos) * arrowHeadSize + endY;

        getElements().add(new LineTo(x1, y1));
        getElements().add(new LineTo(x2, y2));
        getElements().add(new LineTo(endX, endY));
    }
}
